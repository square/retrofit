@file:JvmName("KotlinSerializationConverterFactory")

package com.jakewharton.retrofit2.converter.kotlinx.serialization

import com.jakewharton.retrofit2.converter.kotlinx.serialization.Serializer.FromBytes
import com.jakewharton.retrofit2.converter.kotlinx.serialization.Serializer.FromString
import java.lang.reflect.Type
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.StringFormat
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit

internal class Factory(
  private val contentType: MediaType,
  private val serializer: Serializer
) : Converter.Factory() {
  @Suppress("RedundantNullableReturnType") // Retaining interface contract.
  override fun responseBodyConverter(
    type: Type,
    annotations: Array<out Annotation>,
    retrofit: Retrofit
  ): Converter<ResponseBody, *>? {
    val loader = serializer.serializer(type)
    return DeserializationStrategyConverter(loader, serializer)
  }

  @Suppress("RedundantNullableReturnType") // Retaining interface contract.
  override fun requestBodyConverter(
    type: Type,
    parameterAnnotations: Array<out Annotation>,
    methodAnnotations: Array<out Annotation>,
    retrofit: Retrofit
  ): Converter<*, RequestBody>? {
    val saver = serializer.serializer(type)
    return SerializationStrategyConverter(contentType, saver, serializer)
  }
}

/**
 * Return a [Converter.Factory] which uses Kotlin serialization for string-based payloads.
 *
 * Because Kotlin serialization is so flexible in the types it supports, this converter assumes
 * that it can handle all types. If you are mixing this with something else, you must add this
 * instance last to allow the other converters a chance to see their types.
 */
@JvmName("create")
fun StringFormat.asConverterFactory(contentType: MediaType): Converter.Factory {
  return Factory(contentType, FromString(this))
}

/**
 * Return a [Converter.Factory] which uses Kotlin serialization for byte-based payloads.
 *
 * Because Kotlin serialization is so flexible in the types it supports, this converter assumes
 * that it can handle all types. If you are mixing this with something else, you must add this
 * instance last to allow the other converters a chance to see their types.
 */
@JvmName("create")
fun BinaryFormat.asConverterFactory(contentType: MediaType): Converter.Factory {
  return Factory(contentType, FromBytes(this))
}
