package com.github.dinhn.ads.core

import android.app.Activity
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/** UMP / GDPR consent. Chỉ request ad sau khi canRequestAds() = true. */
object ConsentManager {

    fun request(activity: Activity, onResult: (canRequestAds: Boolean) -> Unit) {
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) AdsLogger.w("Consent form: ${error.message}")
                    onResult(consentInfo.canRequestAds())
                }
            },
            { error ->
                AdsLogger.w("Consent update failed: ${error.message}")
                onResult(consentInfo.canRequestAds())
            },
        )
    }

    /** Cho màn "Privacy settings" / reset khi test. */
    fun reset(activity: Activity) {
        UserMessagingPlatform.getConsentInformation(activity).reset()
    }
}
