package vudt.sdk.ads

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vudt.sdk.ads.exceptions.InitAdsException
import vudt.sdk.ads.exceptions.InvalidAdsIdException

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

  fun loadInterstitialAds(id: String = "", retryCount: Int = 1, delayTimeToRetry: Long = 500L) {
    config?.let {
      if (it.interstitialIds.isEmpty()) {
        throw InvalidAdsIdException("No Interstitial ids found. Check your AdsConfig.")
      }
      if (id.isNotEmpty() && !it.interstitialIds.contains(id)) {
        throw InvalidAdsIdException("$id not found in your AdsConfig.")
      }
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
                GlobalScope.launch (globalJob) {
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