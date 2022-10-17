package vudt.sdk.ads.exceptions

import com.google.android.gms.ads.LoadAdError

class FailedToLoadAdException(val p0: LoadAdError): Throwable(p0.message) {
}