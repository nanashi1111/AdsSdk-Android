package vudt.sdk.ads.views

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.LinearLayoutCompat
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import vudt.sdk.ads.AdsManager
import vudt.sdk.ads.R

class BannerWrapper constructor(context: Context, attrs: AttributeSet?) : LinearLayoutCompat(context, attrs) {

  private val rootLayout: LinearLayoutCompat

  private val adMargin: Int
  private val adBackgroundColor: Int

  init {
    View.inflate(context, R.layout.layout_banner_container, this)
    rootLayout = findViewById(R.id.rootLayout)
    val a: TypedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.BannerWrapper, 0, 0)
    try {
      adMargin = a.getDimensionPixelSize(R.styleable.BannerWrapper_adMargin, 0)
      adBackgroundColor = a.getColor(R.styleable.BannerWrapper_adBackgroundColor, Color.TRANSPARENT)
    } finally {
      a.recycle()
    }
    rootLayout.setBackgroundColor(adBackgroundColor)
  }

  fun loadBanner(id: String, bannerSize: AdSize, callback: BannerCallback? = null) {
    AdsManager.getInstance()?.let {
      val banner = AdView(context).apply {
        setAdSize(bannerSize)
        adUnitId = id
        visibility = View.INVISIBLE
        adListener = object : AdListener() {
          override fun onAdLoaded() {
            super.onAdLoaded()
            visibility = View.VISIBLE
            callback?.onLoaded()
          }

          override fun onAdClicked() {
            super.onAdClicked()
            callback?.onClicked()
          }

          override fun onAdFailedToLoad(p0: LoadAdError) {
            super.onAdFailedToLoad(p0)
            callback?.onFailed(p0)
          }

          override fun onAdClosed() {
            super.onAdClosed()
            callback?.onClosed()
          }
        }
      }
      rootLayout.setPadding(adMargin, adMargin, adMargin, adMargin)
      rootLayout.addView(banner)
      banner.loadAd(it.newAdRequest())
    }
  }
}

interface BannerCallback {
  fun onLoaded()
  fun onClicked()
  fun onFailed(p0: LoadAdError)
  fun onClosed()
}

