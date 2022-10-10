package vudt.sdk.ads.views

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vudt.sdk.ads.R

const val LOADING_TIME = 1000L

class LoadingDialog: DialogFragment() {

  var handler: LoadingDialogCompletionHandler? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.layout_loading, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    isCancelable = false
    viewLifecycleOwner.lifecycleScope.launch {

      when (requireArguments().getString(KEY_TYPE, TYPE_WAIT_SHOW_ADS)) {
        TYPE_WAIT_SHOW_ADS -> {
          delay(LOADING_TIME)
          handler?.onComplete()
          dismissAllowingStateLoss()
        }
        TYPE_WAIT_LOADING_ADS -> {

        }
      }

    }
  }

  override fun onStart() {
    super.onStart()
    dialog?.window?.let {
      it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
      it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
  }

  companion object {
    const val KEY_TYPE = "type"
    const val TYPE_WAIT_SHOW_ADS = "TYPE_WAIT_SHOW_ADS"
    const val TYPE_WAIT_LOADING_ADS = "TYPE_WAIT_LOADING_ADS"

    fun getInstance(type: String): LoadingDialog {
      return LoadingDialog().apply {
        arguments = Bundle().apply {
          putString(KEY_TYPE, type)
        }
      }
    }
  }
}

interface LoadingDialogCompletionHandler {
  fun onComplete()
}