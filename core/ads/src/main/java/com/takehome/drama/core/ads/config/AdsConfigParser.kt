package com.takehome.drama.core.ads.config

import com.takehome.drama.domain.model.ads.AdsConfig
import com.takehome.drama.domain.model.ads.PlacementConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import timber.log.Timber

/**
 * `ads_config` (JSON gõ tay trên Console) → [AdsConfig], **khoan dung theo từng field**: field sai
 * kiểu thì field đó về default, các field khác vẫn đọc. Không dùng `@Serializable` vì một lỗi kiểu
 * ở bất kỳ đâu làm hỏng cả object — tức là gõ sai một placement sẽ vô hiệu luôn kill switch.
 */
object AdsConfigParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parseRaw(raw: String?): AdsConfig =
        parse(raw?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() })

    fun parse(element: JsonElement?): AdsConfig {
        val root = element as? JsonObject ?: return AdsConfig.DEFAULT.also {
            if (element != null) Timber.tag(TAG).w("ads_config is not an object, using embedded default")
        }
        return AdsConfig(
            enable = root["enable"].asBooleanOrNull() ?: true,
            disabledVersions = (root["disabled_versions"] as? List<*>)
                ?.mapNotNull { (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content }
                .orEmpty(),
            placements = (root["placements"] as? JsonObject)
                ?.mapNotNull { (id, value) ->
                    // Placement đọc không ra → bỏ khỏi map ⇒ dùng default của code cho placement đó.
                    (value as? JsonObject)?.get("enable").asBooleanOrNull()?.let { id to PlacementConfig(it) }
                }
                ?.toMap()
                .orEmpty(),
        )
    }

    /** `true`/`false` hoặc chuỗi `"true"`/`"false"` (Console hay gõ thành chuỗi). Còn lại → null. */
    private fun JsonElement?.asBooleanOrNull(): Boolean? {
        val primitive = this as? JsonPrimitive ?: return null
        return if (primitive.isString) {
            primitive.contentOrNull?.lowercase()?.toBooleanStrictOrNull()
        } else {
            primitive.booleanOrNull
        }
    }

    const val TAG = "AdsPolicy"
}
