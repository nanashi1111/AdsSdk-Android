package vudt.sdk.ads.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import vudt.sdk.ads.data.models.State

abstract class UseCase<Output, Params> {
  operator fun invoke(param: Params): Flow<State<Output>> {
    return buildFlow(param)
      .onStart {
        emit(State.LoadingState)
      }.catch { cause: Throwable ->
        emit(State.ErrorState(cause))
      }
  }

  abstract fun buildFlow(param: Params): Flow<State<Output>>
}

