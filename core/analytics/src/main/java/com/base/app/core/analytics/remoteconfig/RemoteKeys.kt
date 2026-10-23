package com.base.app.core.analytics.remoteconfig

import kotlinx.serialization.json.Json

/**
 * Định nghĩa tất cả Remote Config keys.
 *
 * ```
 * val FEATURE_ENABLED = BooleanKey(key = "feature_enabled", default = false)
 * val MAX_RETRY = LongKey(key = "max_retry", default = 3L)
 * val APP_CONFIG = JsonKey(key = "app_config", default = AppConfigRc(), serializer = AppConfigRc.serializer())
 * ```
 * Khai báo xong nhớ thêm vào [ALL_KEYS].
 */
object RemoteKeys {

    val ALL_KEYS: List<RemoteConfigKey<*>> = listOf(
    )

    private val defaultsJson = Json { encodeDefaults = true }

    /** Default của code, seed vào Firebase bằng `setDefaultsAsync`. */
    fun defaultsMap(): Map<String, Any> = ALL_KEYS.associate { key ->
        key.key to when (key) {
            is StringKey -> key.default
            is LongKey -> key.default
            is BooleanKey -> key.default
            is JsonKey<*> -> key.encodeDefault(defaultsJson)
        }
    }

    private fun <T> JsonKey<T>.encodeDefault(json: Json): String = json.encodeToString(serializer, default)
}
