package vudt.sdk.ads

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import vudt.sdk.ads.data.AdsRepository
import vudt.sdk.ads.data.AdsRepositoryImpl
import vudt.sdk.ads.data.models.AdsType
import vudt.sdk.ads.data.models.State
import vudt.sdk.ads.domain.*
import vudt.sdk.ads.views.LoadingDialog

class AdsManager private constructor(context: Context) {

  //Repo
  private val adsRepository: AdsRepository = AdsRepositoryImpl()

  //Usecases
  private val loadInterstitialAdUseCase = LoadInterstitial(context, adsRepository)
  private val markAdsUseCase = MarkAds(adsRepository)

  var job = Job()

  var config: AdsConfig? = null
    private set

  var loadingDialog: LoadingDialog? = null

  fun prefetchInterstitial(
    coroutineScope: CoroutineScope = GlobalScope, id: String,
    maxRetry: Int = DEFAULT_RETRY_COUNT,
    delayPerRetry: Long = DEFAULT_DELAY_RETRY,
    timeout: Long = DEFAULT_TIMEOUT
  ) {
    coroutineScope.launch(job) {
      loadInterstitialAdUseCase.invoke(LoadInterstitial.Params(id, retryCount = maxRetry, timeDelayToRetry = delayPerRetry, timeout = timeout))
        .collectLatest {
          when (it) {
            is State.LoadingState -> {
              Log.d("PrefetchAd", "Loading")
            }
            is State.DataState -> {
              Log.d("PrefetchAd", "Success: ${it.data.hashCode()}")
            }
            is State.ErrorState -> {
              Log.d("PrefetchAd", "Error: ${it.exception.message}")
            }
          }
        }
    }
  }

  @DelicateCoroutinesApi
  fun tryToShowInterstitial(
    coroutineScope: CoroutineScope = GlobalScope, context: AppCompatActivity, id: String,
    maxRetry: Int = DEFAULT_RETRY_COUNT,
    delayPerRetry: Long = DEFAULT_DELAY_RETRY,
    timeout: Long = DEFAULT_TIMEOUT ,
    onFailed: ((Throwable) -> Unit)? = null,
    onClosed: (() -> Unit)? = null
  ) {
    coroutineScope.launch(job) {
      loadInterstitialAdUseCase.invoke(LoadInterstitial.Params(id, retryCount = maxRetry, timeDelayToRetry = delayPerRetry, timeout = timeout)).collectLatest {
        when (it) {
          is State.LoadingState -> {
            invalidateDialogThenShow(context)
          }
          is State.DataState -> {
            dismissLoadingDialog()
            val ads = it.data
            onClosed?.let {
              ads.interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                  super.onAdDismissedFullScreenContent()
                  coroutineScope.launch {
                    markAdsUseCase.invoke(MarkAds.Params(id, null)).collectLatest {
                      Log.d("markAds", "$id has been marked as null")
                      prefetchInterstitial(coroutineScope, id, maxRetry, delayPerRetry)
                    }
                  }
                  onClosed.invoke()
                }
              }
            } ?: run {
              ads.interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                  super.onAdDismissedFullScreenContent()
                  coroutineScope.launch {
                    markAdsUseCase.invoke(MarkAds.Params(id, null)).collectLatest {
                      Log.d("markAds", "$id has been marked as null")
                      prefetchInterstitial(coroutineScope, id, maxRetry, delayPerRetry)
                    }
                  }
                }
              }
            }
            ads.interstitialAd?.show(context)
          }
          is State.ErrorState -> {
            dismissLoadingDialog()
            it.exception.printStackTrace()
            onFailed?.invoke(it.exception)
          }
        }
      }
    }
  }

  fun newAdRequest() = adsRepository.newAdRequest()

  private fun invalidateDialogThenShow(context: AppCompatActivity) {
    loadingDialog?.dismissAllowingStateLoss()
    loadingDialog = null
    loadingDialog = LoadingDialog()
    loadingDialog?.show(context.supportFragmentManager, "${loadingDialog?.javaClass?.canonicalName}")
  }

  private fun dismissLoadingDialog() {
    loadingDialog?.dismissAllowingStateLoss()
    loadingDialog = null
  }

  fun onDestroy() {
    job.cancel()
    INSTANCE = null
  }

  companion object {

    private var INSTANCE: AdsManager? = null

    fun init(context: Context, adsConfig: AdsConfig) {
      MobileAds.initialize(context) {
        INSTANCE = AdsManager(context).apply {
          config = adsConfig
          adsRepository.setAdIds(AdsType.INTERSTITIAL, adsConfig.interstitialIds)
          adsRepository.setAdIds(AdsType.BANNER, adsConfig.bannerIds)
          adsConfig.testDevices.let { testDevices ->
            val config = RequestConfiguration.Builder().setTestDeviceIds(testDevices).build()
            MobileAds.setRequestConfiguration(config)
          }

          Log.d("AdConfig", Gson().toJson(adsConfig))
        }
      }
    }

    fun getInstance() = INSTANCE

    fun available() = (getInstance() != null)
  }

}