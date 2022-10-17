package vudt.sdk.ads.views

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import vudt.sdk.ads.R


class LoadingDialog: DialogFragment() {

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.layout_loading, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    isCancelable = false
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