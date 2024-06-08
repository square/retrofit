package retrofit2.converter.kotlinx.serialization.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import retrofit2.Converter
import retrofit2.converter.kotlinx.serialization.Factory

/**
 * Return a [Converter.Factory] which uses Kotlin serialization for Json-based payloads.
 *
 * Because Kotlin serialization is so flexible in the types it supports, this converter assumes
 * that it can handle all types. If you are mixing this with something else, you must add this
 * instance last to allow the other converters a chance to see their types.
 */
@ExperimentalSerializationApi
@JvmName("create")
fun Json.asConverterFactory(): Converter.Factory {
  val contentType = MediaType.get("application/json; charset=UTF-8")
  return Factory(contentType, SerializerFromJson(this))
}
