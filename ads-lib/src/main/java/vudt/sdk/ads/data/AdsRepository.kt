package vudt.sdk.ads.data

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import vudt.sdk.ads.data.models.Ad
import vudt.sdk.ads.data.models.AdsType
import vudt.sdk.ads.data.models.InterstitialWrapper
import vudt.sdk.ads.exceptions.FailedToLoadAdException

interface AdsRepository {

  fun setAdIds(type: AdsType, ids: List<String>)

  fun getAdIds(type: AdsType): List<String>

  fun isValid(id: String, type: AdsType): Boolean

  fun loadInterstitial(context: Context, id: String): Flow<InterstitialWrapper>

  fun interstitialRepo(): Map<String, InterstitialAd>

  fun markInterstitialAd(id: String, ad: InterstitialAd?)

  fun newAdRequest(): AdRequest

}

class AdsRepositoryImpl : AdsRepository {

  val ads = mutableListOf<Ad>()

  private val interstitialMap = HashMap<String, InterstitialAd>()

  override fun setAdIds(type: AdsType, ids: List<String>) {
    ads.addAll(ids.map { adsId ->
      Ad(type, adsId)
    })
  }

  override fun getAdIds(type: AdsType): List<String> {
    return ads.filter {
      it.type == type
    }.map {
      it.id
    }
  }

  override fun isValid(id: String, type: AdsType): Boolean {
    return getAdIds(type).contains(id)
  }

  override fun newAdRequest() = AdRequest.Builder().build()

  override fun loadInterstitial(context: Context, id: String): Flow<InterstitialWrapper> {
    val adsFlow = callbackFlow {
      InterstitialAd.load(
        context, id, newAdRequest(), object : InterstitialAdLoadCallback() {
          override fun onAdLoaded(ad: InterstitialAd) {
            super.onAdLoaded(ad)
            interstitialMap[id] = ad
            trySend(InterstitialWrapper(ad, null))
            Log.d("Ad", "OnAdLoaded")
          }

          override fun onAdFailedToLoad(p0: LoadAdError) {
            super.onAdFailedToLoad(p0)
            interstitialMap.remove(id)
            Log.d("Ad", "OnAdFailedToLoad: $p0")
            trySend(InterstitialWrapper(null, FailedToLoadAdException(p0)))
          }
        }
      )
      awaitClose {  }

    }
    return adsFlow
  }

  override fun markInterstitialAd(id: String, ad: InterstitialAd?) {
    if (ad == null) {
      interstitialMap.remove(id)
    } else {
      interstitialMap[id] = ad
    }
  }

  override fun interstitialRepo(): Map<String, InterstitialAd> {
    return interstitialMap
  }

}