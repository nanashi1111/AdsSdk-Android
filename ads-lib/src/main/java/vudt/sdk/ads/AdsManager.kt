package vudt.sdk.ads

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import vudt.sdk.ads.data.AdsRepository
import vudt.sdk.ads.data.AdsRepositoryImpl
import vudt.sdk.ads.data.models.AdsType
import vudt.sdk.ads.data.models.State
import vudt.sdk.ads.domain.DEFAULT_DELAY_RETRY
import vudt.sdk.ads.domain.DEFAULT_TIMEOUT
import vudt.sdk.ads.domain.LoadInterstitial
import vudt.sdk.ads.domain.MarkAds
import vudt.sdk.ads.views.LoadingDialog

class AdsManager private constructor(context: Context) {

  //Repo
  private val adsRepository: AdsRepository = AdsRepositoryImpl()

  //Usecases
  private val loadInterstitialAdUseCase = LoadInterstitial(context, adsRepository)
  private val markAdsUseCase = MarkAds(adsRepository)

  var config: AdsConfig? = null
    private set

  var loadingDialog: LoadingDialog? = null

  @DelicateCoroutinesApi
  fun loadInterstitialThenShow(coroutineScope: CoroutineScope = GlobalScope, context: AppCompatActivity, id: String,
                               maxRetry: Int = 0,
                               delayPerRetry: Long = DEFAULT_DELAY_RETRY,
                               timeout: Long = DEFAULT_TIMEOUT,
                               onFailed: ((Throwable) -> Unit)? = null,
                               onClosed: (() -> Unit)? = null) {
    coroutineScope.launch {
      loadInterstitialAdUseCase.invoke(LoadInterstitial.Params(id, retryCount = maxRetry, timeDelayToRetry = delayPerRetry, timeout = timeout)).collectLatest {
        when (it) {
          is State.LoadingState -> {
            invalidateDialogThenShow(context)
          }
          is State.DataState -> {
            dismissLoadingDialog()
            val ads = it.data
            onClosed?.let {
              ads.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                  super.onAdDismissedFullScreenContent()
                  coroutineScope.launch { markAdsUseCase.invoke(MarkAds.Params(id, null)).collectLatest {  } }
                  onClosed.invoke()
                }
              }
            } ?: run {
              ads.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                  super.onAdDismissedFullScreenContent()
                  coroutineScope.launch { markAdsUseCase.invoke(MarkAds.Params(id, null)).collectLatest {  } }
                }
              }
            }
            ads.show(context)
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

  companion object {

    private var INSTANCE: AdsManager? = null

    fun init(context: Context, adsConfig: AdsConfig) {
      MobileAds.initialize(context) {
        INSTANCE = AdsManager(context).apply {
          config = adsConfig
          adsRepository.setAdIds(AdsType.INTERSTITIAL, adsConfig.interstitialIds)
          adsConfig.testDevices.let { testDevices ->
            val config = RequestConfiguration.Builder().setTestDeviceIds(testDevices).build()
            MobileAds.setRequestConfiguration(config)
          }
        }
      }
    }

    fun getInstance() = INSTANCE

    fun available() = (getInstance() != null)
  }

}