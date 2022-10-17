package vudt.sdk.ads.domain

import com.google.android.gms.ads.interstitial.InterstitialAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import vudt.sdk.ads.data.AdsRepository
import vudt.sdk.ads.data.models.State

class MarkAds constructor(private val adsRepository: AdsRepository): UseCase<Unit, MarkAds.Params>() {

  class Params(val id: String, val ad: InterstitialAd?)

  override fun buildFlow(param: Params): Flow<State<Unit>> {
    return flow {
      adsRepository.markInterstitialAd(param.id, param.ad)
      emit(State.DataState(Unit))
    }.flowOn(Dispatchers.IO)
  }
}