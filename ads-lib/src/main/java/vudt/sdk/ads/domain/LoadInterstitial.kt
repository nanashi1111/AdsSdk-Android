package vudt.sdk.ads.domain

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.interstitial.InterstitialAd
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import vudt.sdk.ads.AdsManager
import vudt.sdk.ads.data.AdsRepository
import vudt.sdk.ads.data.models.AdsType
import vudt.sdk.ads.data.models.InterstitialWrapper
import vudt.sdk.ads.data.models.State
import vudt.sdk.ads.exceptions.InitAdsException
import vudt.sdk.ads.exceptions.InvalidAdsIdException
import vudt.sdk.ads.exceptions.TimeoutException

class LoadInterstitial constructor(
  private val context: Context,
  private val adsRepository: AdsRepository
) : UseCase<InterstitialWrapper, LoadInterstitial.Params>() {

  class Params(val id: String, val retryCount: Int, val timeDelayToRetry: Long, val timeout: Long)

  override fun buildFlow(param: Params): Flow<State<InterstitialWrapper>> {

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

    adsRepository.interstitialRepo()[param.id]?.let { ad ->
      return flow {
        delay(500)
        emit(State.DataState(InterstitialWrapper(ad, null)))
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
      val ads = it as InterstitialWrapper
      flow {
        Log.d("Ad", "TimeLoad = ${System.currentTimeMillis() - startTime}")
        if (ads.interstitialAd != null) {
          emit(State.DataState(ads))
        } else {
          throw ads.throwable!!
          //emit(State.ErrorState(ads.throwable!!))
        }
      }
    }.retryWhen { cause, attempt ->
      val canRetry = cause !is TimeoutException && attempt < param.retryCount && System.currentTimeMillis() - startTime < param.timeout
      Log.d("canRetry", "cause: ${cause.message} ; canRetry = $canRetry ; attempt = $attempt ; ellapsedTime: ${(System.currentTimeMillis() - startTime)}")
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