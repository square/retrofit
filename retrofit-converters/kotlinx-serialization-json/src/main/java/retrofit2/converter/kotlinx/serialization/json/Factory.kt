package retrofit2.converter.kotlinx.serialization.json

import java.lang.reflect.Type
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit

@ExperimentalSerializationApi
internal class Factory(
  private val json: Json,
) : Converter.Factory() {

  @Suppress("RedundantNullableReturnType") // Retaining interface contract.
  override fun responseBodyConverter(
    type: Type,
    annotations: Array<out Annotation>,
    retrofit: Retrofit,
  ): Converter<ResponseBody, *>? {
    val loader = serializer(type)
    return ResponseBodyConverter(json, loader)
  }

  @Suppress("RedundantNullableReturnType") // Retaining interface contract.
  override fun requestBodyConverter(
    type: Type,
    parameterAnnotations: Array<out Annotation>,
    methodAnnotations: Array<out Annotation>,
    retrofit: Retrofit,
  ): Converter<*, RequestBody>? {
    val saver = serializer(type)
    return RequestBodyConverter(json, saver)
  }

  private fun serializer(type: Type) = json.serializersModule.serializer(type)
}

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
  return Factory(this)
}
