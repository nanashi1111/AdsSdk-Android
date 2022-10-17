package vudt.sdk.ads.domain

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.interstitial.InterstitialAd
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import vudt.sdk.ads.AdsManager
import vudt.sdk.ads.data.AdsRepository
import vudt.sdk.ads.data.models.AdsType
import vudt.sdk.ads.data.models.State
import vudt.sdk.ads.exceptions.InitAdsException
import vudt.sdk.ads.exceptions.InvalidAdsIdException
import vudt.sdk.ads.exceptions.TimeoutException

class LoadInterstitial constructor(
  private val context: Context,
  private val adsRepository: AdsRepository
) : UseCase<InterstitialAd, LoadInterstitial.Params>() {

  class Params(val id: String, val retryCount: Int = 0, val timeDelayToRetry: Long, val timeout: Long)

  override fun buildFlow(param: Params): Flow<State<InterstitialAd>> {

    if (!AdsManager.available()) {
      return flow {
        emit(State.ErrorState(InitAdsException()))
      }
    }

    if (!adsRepository.isValid(param.id, AdsType.INTERSTITIAL)) {
      return flow {
        emit(State.ErrorState(InvalidAdsIdException()))
      }
    }

    if (adsRepository.interstitialRepo().get(param.id) != null) {
      return flow {
        emit(State.DataState(adsRepository.interstitialRepo().get(param.id)!!))
      }
    }

    adsRepository.interstitialRepo().get(param.id)?.let { ad ->
      return flow {
        emit(State.DataState(ad))
      }
    }

    val timeoutFlow: Flow<Long> = flow {
      delay(param.timeout)
      emit(System.currentTimeMillis())
    }

    val startTime = System.currentTimeMillis()
    val adsFlow = adsRepository.loadInterstitial(context, param.id)

    val resultFlow = merge(timeoutFlow, adsFlow).flatMapLatest {
      if (it is Long) {
        Log.d("Ad", "TimeLoad Timeout = ${System.currentTimeMillis() - startTime}")
        throw TimeoutException()
      }
      val ads = it as InterstitialAd
      flow {
        Log.d("Ad", "TimeLoad = ${System.currentTimeMillis() - startTime}")
        emit(State.DataState(ads))
      }
    }.retryWhen { cause, attempt ->
        val canRetry = cause !is TimeoutException && attempt < param.retryCount && System.currentTimeMillis() - startTime < param.timeDelayToRetry
        if (canRetry) {
          delay(param.timeDelayToRetry)
          true
        } else {
          false
        }

      }.take(1)
    return resultFlow
  }
}