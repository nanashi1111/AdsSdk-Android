package vudt.sdk.ads.utils

import vudt.sdk.ads.AdsManager
import vudt.sdk.ads.exceptions.InitAdsException
import vudt.sdk.ads.exceptions.InvalidAdsIdException

fun validateInterstitialAdsId(id: String, type: AdType): Boolean {
  if (!AdsManager.available()) {
    throw InitAdsException()
  }
  AdsManager.getInstance()?.config?.let {

    when(type) {
      AdType.INTERSTITIAL -> {
        if (it.interstitialIds.isEmpty()) {
          throw InvalidAdsIdException("No Interstitial ids found. Check your AdsConfig.")
        }
        if (id.isNotEmpty() && !it.interstitialIds.contains(id)) {
          throw InvalidAdsIdException("$id not found in your AdsConfig.")
        }
        return true
      }
    }


  } ?: run {
    throw InitAdsException()
  }
}

enum class AdType {
  INTERSTITIAL
}