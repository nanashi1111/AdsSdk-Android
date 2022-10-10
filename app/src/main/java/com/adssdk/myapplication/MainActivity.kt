package com.adssdk.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vudt.sdk.ads.AdsConfig
import vudt.sdk.ads.AdsManager
import vudt.sdk.ads.views.LoadingDialog

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    AdsManager.init(this, AdsConfig(
      appId = "ca-app-pub-3940256099942544~3347511713",
      interstitialIds = listOf("ca-app-pub-3940256099942544/1033173712")
    ))
    lifecycleScope.launch {
      delay(1000)
      AdsManager.getInstance()?.loadInterstitialThenShow(this@MainActivity, onFailed = {
        Toast.makeText(this@MainActivity, "Failed", Toast.LENGTH_SHORT).show()
      }, onAdDismissed = {
        Toast.makeText(this@MainActivity, "Dismiss", Toast.LENGTH_SHORT).show()
      })
    }

  }
}