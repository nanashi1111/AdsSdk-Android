package vudt.sdk.ads

data class AdsConfig(
  val appId: String,
  val bannerIds: List<String> = emptyList(),
  val interstitialIds: List<String> = emptyList(),
  val testDevices: List<String> = emptyList()
)