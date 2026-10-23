package com.takehome.drama.data.remote.dto.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import timber.log.Timber

/*
 * Serializer chịu lỗi cho payload catalogue. Server không chặt (field thiếu, kiểu đổi, key mới),
 * và mất một phần tử thì chấp nhận được còn mất cả danh sách thì không — nên từng phần tử được
 * decode riêng, phần tử hỏng bị bỏ và log lại.
 */

/** Mảng mà phần tử hỏng bị bỏ qua, các phần tử còn lại giữ nguyên thứ tự. */
class SkipInvalidListSerializer<T>(private val element: KSerializer<T>) : KSerializer<List<T>> {
    private val delegate = ListSerializer(element)
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): List<T> = decoder.decodeEach(element).filterNotNull()

    override fun serialize(encoder: Encoder, value: List<T>) = delegate.serialize(encoder, value)
}

/**
 * Như [SkipInvalidListSerializer] nhưng phần tử hỏng thành `null` tại chỗ, để vị trí gốc trong
 * mảng còn dùng được làm định danh (tập phim: khoá của watch progress).
 */
open class PositionalListSerializer<T : Any>(private val element: KSerializer<T>) : KSerializer<List<T?>> {
    private val delegate = ListSerializer(element.nullable)
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): List<T?> = decoder.decodeEach(element)

    override fun serialize(encoder: Encoder, value: List<T?>) = delegate.serialize(encoder, value)
}

/** Mảng sai kiểu → rỗng; phần tử hỏng → null (đã log). */
private fun <T> Decoder.decodeEach(element: KSerializer<T>): List<T?> {
    val json = asJson()
    val array = json.decodeJsonElement() as? JsonArray ?: run {
        Timber.w("Expected array of %s, got something else", element.descriptor.serialName)
        return emptyList()
    }
    return array.map { item ->
        runCatching { json.json.decodeFromJsonElement(element, item) }
            .onFailure { Timber.w(it, "Skip invalid %s", element.descriptor.serialName) }
            .getOrNull()
    }
}

/**
 * Text đa ngôn ngữ: bình thường là object `{ "en": "…" }`; chấp nhận cả string phẳng (key rỗng)
 * phòng server đổi. Value không phải string bị bỏ.
 */
object LocalizedMapSerializer : KSerializer<Map<String, String>> {
    private val delegate = MapSerializer(String.serializer(), String.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): Map<String, String> =
        when (val element = decoder.asJson().decodeJsonElement()) {
            is JsonObject -> element.mapNotNull { (key, value) -> value.stringOrNull()?.let { key to it } }.toMap()
            is JsonPrimitive -> element.stringOrNull()?.let { mapOf("" to it) }.orEmpty()
            else -> emptyMap()
        }

    override fun serialize(encoder: Encoder, value: Map<String, String>) = delegate.serialize(encoder, value)
}

/**
 * `sources`: ngôn ngữ → chất lượng → URL. Key chất lượng nào cũng nhận (`1080p`, `2160p`, `abc`);
 * rendition hỏng (null, object, số, chuỗi rỗng) bị bỏ, tập vẫn giữ các rendition còn lại.
 */
object SourcesSerializer : KSerializer<Map<String, Map<String, String>>> {
    private val delegate = MapSerializer(String.serializer(), MapSerializer(String.serializer(), String.serializer()))
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): Map<String, Map<String, String>> {
        val root = decoder.asJson().decodeJsonElement() as? JsonObject ?: return emptyMap()
        return root.mapNotNull { (language, renditions) ->
            val byQuality = (renditions as? JsonObject)
                ?.mapNotNull { (quality, url) -> url.stringOrNull()?.takeIf { it.isNotBlank() }?.let { quality to it } }
                ?.toMap()
            byQuality?.takeIf { it.isNotEmpty() }?.let { language to it }
        }.toMap()
    }

    override fun serialize(encoder: Encoder, value: Map<String, Map<String, String>>) =
        delegate.serialize(encoder, value)
}

private fun Decoder.asJson(): JsonDecoder =
    this as? JsonDecoder ?: error("Catalogue serializers only support JSON")

private fun JsonElement.stringOrNull(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content
