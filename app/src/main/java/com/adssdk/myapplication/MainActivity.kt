package com.adssdk.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vudt.sdk.ads.AdsConfig
import vudt.sdk.ads.AdsManager
import vudt.sdk.ads.data.models.State
import vudt.sdk.ads.domain.LoadInterstitial
import vudt.sdk.ads.views.LoadingDialog

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    AdsManager.init(
      this, AdsConfig(
        appId = "ca-app-pub-3940256099942544~3347511713",
        interstitialIds = listOf("ca-app-pub-3940256099942544/1033173712")
      )
    )

    findViewById<View>(R.id.btShowAds).setOnClickListener {
      loadThenShow()
    }
  }

  private fun loadThenShow() {
    AdsManager.getInstance()!!.loadInterstitialThenShow(
      coroutineScope = lifecycleScope,
      context = this@MainActivity,
      id = "ca-app-pub-3940256099942544/1033173712",
      onFailed = {
        Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_SHORT).show()
      }, onClosed = {
        Toast.makeText(this@MainActivity, "Dismiss", Toast.LENGTH_SHORT).show()
      }
    )
  }
}