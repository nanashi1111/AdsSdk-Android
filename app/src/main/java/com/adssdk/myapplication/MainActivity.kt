package com.adssdk.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vudt.sdk.ads.AdsConfig
import vudt.sdk.ads.AdsManager
import vudt.sdk.ads.views.BannerWrapper

class MainActivity : AppCompatActivity() {

  private val banner: BannerWrapper by lazy {
    findViewById(R.id.banner)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    AdsManager.init(
      this, AdsConfig(
        appId = "ca-app-pub-3940256099942544~3347511713",
        interstitialIds = listOf("ca-app-pub-3940256099942544/1033173712"),
        bannerIds = listOf("ca-app-pub-3940256099942544/6300978111")
      )
    )

    findViewById<View>(R.id.btShowAds).setOnClickListener {
      loadThenShow()
    }

    lifecycleScope.launch {
      delay(1000)
      AdsManager.getInstance()?.prefetchInterstitial(this, "ca-app-pub-3940256099942544/1033173712")
      banner.loadBanner("ca-app-pub-3940256099942544/6300978111", AdSize.MEDIUM_RECTANGLE)
    }

  }

  private fun loadThenShow() {
    AdsManager.getInstance()?.tryToShowInterstitial(
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

  override fun onDestroy() {
    super.onDestroy()
    AdsManager.getInstance()?.onDestroy()
  }
}