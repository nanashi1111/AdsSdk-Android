package vudt.sdk.ads

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vudt.sdk.ads.exceptions.InitAdsException
import vudt.sdk.ads.utils.AdType
import vudt.sdk.ads.utils.validateInterstitialAdsId
import vudt.sdk.ads.views.LoadingDialog
import vudt.sdk.ads.views.LoadingDialogCompletionHandler

class AdsManager private constructor(val context: Context) {

  //Interstitial with id
  val interstitialAdsMap = HashMap<String, InterstitialAd>()

  //Retry information
  val interstitialRetryMap = HashMap<String, Int>()


  var config: AdsConfig? = null
    private set(value) {
      field = value
    }

  val globalJob = Job()

  fun loadInterstitialThenShow(activity: AppCompatActivity, id: String = "", retryCount: Int = 0, delayTimeToRetry: Long = 500L, onFailed: (() -> Unit)? = null, onAdDismissed: (() -> Unit)?) {
    validateInterstitialAdsId(id, AdType.INTERSTITIAL)
    val adsId = if (id.isEmpty()) {
      config!!.interstitialIds.first()
    } else {
      id
    }
    if (interstitialAdsMap[adsId] == null) {
      val loadingDialog = LoadingDialog.getInstance(LoadingDialog.TYPE_WAIT_LOADING_ADS)
      loadingDialog.show(activity.supportFragmentManager, loadingDialog.javaClass.canonicalName)
      InterstitialAd.load(
        context, adsId, provideAdsRequest(), object : InterstitialAdLoadCallback() {
          override fun onAdLoaded(p0: InterstitialAd) {
            super.onAdLoaded(p0)
            interstitialAdsMap[adsId] = p0
            interstitialRetryMap[adsId] = 0
            p0.fullScreenContentCallback = object : FullScreenContentCallback() {
              override fun onAdDismissedFullScreenContent() {
                loadingDialog.dismissAllowingStateLoss()
                onAdDismissed?.invoke()
              }
            }
            p0.show(activity)
          }

          override fun onAdFailedToLoad(p0: LoadAdError) {
            super.onAdFailedToLoad(p0)
            interstitialAdsMap.remove(adsId)

            if (retryCount > 0) {
              val retriedCount = interstitialRetryMap[adsId] ?: 0
              if (retryCount > retriedCount) {
                GlobalScope.launch(globalJob) {
                  delay(delayTimeToRetry)
                  interstitialRetryMap[adsId] = 1 + retriedCount
                  loadInterstitialAds(id, retryCount, delayTimeToRetry)
                }
              } else {
                loadingDialog.dismissAllowingStateLoss()
                onFailed?.invoke()
              }
            } else {
              loadingDialog.dismissAllowingStateLoss()
              onFailed?.invoke()
            }
          }
        }
      )
    } else {
      val loadingDialog = LoadingDialog.getInstance(LoadingDialog.TYPE_WAIT_SHOW_ADS)
      loadingDialog.handler = object :LoadingDialogCompletionHandler{
        override fun onComplete() {
          showInterstitialAds(activity, id, onAdDismissed)
        }
      }
      loadingDialog.show(activity.supportFragmentManager, loadingDialog.javaClass.canonicalName)
    }
  }

  fun showInterstitialAds(activity: AppCompatActivity, id: String = "", onAdDismissed: (() -> Unit)? = null) {
    validateInterstitialAdsId(id, AdType.INTERSTITIAL)
    val adsId = if (id.isEmpty()) {
      config!!.interstitialIds.first()
    } else {
      id
    }

    interstitialAdsMap[adsId]?.let { ads ->
      val loadingDialog = LoadingDialog().apply {
        handler = object : LoadingDialogCompletionHandler {
          override fun onComplete() {
            ads.fullScreenContentCallback = object : FullScreenContentCallback() {
              override fun onAdDismissedFullScreenContent() {
                interstitialAdsMap.remove(adsId)
                onAdDismissed?.invoke()
              }
            }
            ads.show(activity)
          }
        }
      }

      loadingDialog.show(activity.supportFragmentManager, loadingDialog.javaClass.canonicalName)
    }
  }

  fun loadInterstitialAds(id: String = "", retryCount: Int = 1, delayTimeToRetry: Long = 500L) {
    config?.let {
      validateInterstitialAdsId(id, AdType.INTERSTITIAL)
      val adsId = if (id.isEmpty()) {
        it.interstitialIds.first()
      } else {
        id
      }
      InterstitialAd.load(
        context, adsId, provideAdsRequest(), object : InterstitialAdLoadCallback() {
          override fun onAdLoaded(p0: InterstitialAd) {
            super.onAdLoaded(p0)
            interstitialAdsMap[adsId] = p0
            interstitialRetryMap[adsId] = 0
          }

          override fun onAdFailedToLoad(p0: LoadAdError) {
            super.onAdFailedToLoad(p0)
            interstitialAdsMap.remove(adsId)

            if (retryCount > 0) {
              val retriedCount = interstitialRetryMap[adsId] ?: 0
              if (retryCount > retriedCount) {
                GlobalScope.launch(globalJob) {
                  delay(delayTimeToRetry)
                  interstitialRetryMap[adsId] = 1 + retriedCount
                  loadInterstitialAds(id, retryCount, delayTimeToRetry)
                }
              }
            }
          }
        }
      )
    } ?: run {
      throw InitAdsException()
    }

    fun destroy() {
      globalJob.cancel()
      AdsManager.INSTANCE = null
    }

  }

  private fun provideAdsRequest(): AdRequest = AdRequest.Builder().build()

  companion object {

    private var INSTANCE: AdsManager? = null

    fun init(context: Context, adsConfig: AdsConfig) {
      MobileAds.initialize(context) {

        INSTANCE = AdsManager(context).apply {
          config = adsConfig
        }
        //Set Test device
        getInstance()?.config?.testDevices?.let { testDevices ->
          val config = RequestConfiguration.Builder().setTestDeviceIds(testDevices).build()
          MobileAds.setRequestConfiguration(config)
        }
      }
    }

    fun getInstance() = INSTANCE

    fun available() = (getInstance() != null)
  }

}