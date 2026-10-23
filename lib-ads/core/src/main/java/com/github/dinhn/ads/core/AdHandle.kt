package com.github.dinhn.ads.core

/**
 * Handle "mờ" tới một full-screen ad ĐÃ load xong (interstitial / app open / rewarded).
 * App giữ handle này rồi show sau. [payload] là ad object của network (vd AdMob InterstitialAd),
 * chỉ adapter mới hiểu — core/app coi như opaque. [adUnitId] để log biết là ad nào lúc show.
 * [placement] = tên placement (vd `InterHome`) do tầng manager truyền vào lúc load, để log lúc
 * show cũng kèm được placement (show lấy id từ handle chứ không qua load call).
 */
class AdHandle(val payload: Any, val adUnitId: String = "", val placement: String = "")
