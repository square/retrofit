/*
 * Copyright (C) 2017 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit2.adapter.sse.internal

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import retrofit2.Call
import retrofit2.Converter
import retrofit2.Response
import retrofit2.adapter.sse.EventSource
import retrofit2.adapter.sse.SseCallback

@Suppress("NOTHING_TO_INLINE")
private inline fun <T : Any> conversionError(value: T, type: Type): Nothing = error("Failed to convert $value to $type, actual type is ${value.javaClass}")

private fun Response<ResponseBody>.asSse(listener: EventSourceListener) {
  val okhttpResponse = raw().newBuilder().body(body() ?: error("Response body is null")).build()
  EventSources.processResponse(okhttpResponse, listener)
}

private fun Type.acceptsString(): Boolean =
  when (this) {
    String::class.java -> true
    Object::class.java -> true
    CharSequence::class.java -> true
    Comparable::class.java -> true
    is ParameterizedType -> rawType === Comparable::class.java && actualTypeArguments[0].acceptsString()
    is WildcardType -> upperBounds[0].acceptsString()
    else -> false
  }

internal class RealEventSource<ID : Any, TYPE : Any, DATA : Any>(
  private val idType: Type,
  private val typeType: Type,
  private val dataType: Type,
  private val idConverter: Converter<ResponseBody, ID>,
  private val typeConverter: Converter<ResponseBody, TYPE>,
  private val dataConverter: Converter<ResponseBody, DATA>,
  private val call: Call<ResponseBody>,
) : EventSource<ID, TYPE, DATA> {
  override fun request(): Request = call.request()

  override fun cancel() = call.cancel()

  override fun subscribe(callback: SseCallback<ID, TYPE, DATA>) {
    call.enqueue(
      object : retrofit2.Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
          response.asSse(
            object : EventSourceListener() {
              override fun onOpen(eventSource: okhttp3.sse.EventSource, response: okhttp3.Response) {
                callback.onOpen(this@RealEventSource)
              }

              override fun onEvent(eventSource: okhttp3.sse.EventSource, id: String?, type: String?, data: String) {
                val convertedId = convertId(id)
                val convertedType = convertType(type)
                val convertedData = convertData(data)
                callback.onEvent(this@RealEventSource, convertedId, convertedType, convertedData)
              }

              override fun onClosed(eventSource: okhttp3.sse.EventSource) {
                callback.onClosed(this@RealEventSource)
              }

              override fun onFailure(eventSource: okhttp3.sse.EventSource, t: Throwable?, response: okhttp3.Response?) {
                callback.onFailure(this@RealEventSource, t)
              }
            },
          )
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
          callback.onFailure(this@RealEventSource, t)
        }
      },
    )
  }

  private fun convertId(id: String?): ID? {
    @Suppress("UNCHECKED_CAST")
    return when {
      idType.acceptsString() -> id as ID?
      id != null -> idConverter.convert(id.toResponseBody()) ?: conversionError(id, idType)
      else -> null
    }
  }

  private fun convertType(type: String?): TYPE? {
    @Suppress("UNCHECKED_CAST")
    return when {
      typeType.acceptsString() -> type as TYPE?
      type != null -> typeConverter.convert(type.toResponseBody()) ?: conversionError(type, typeType)
      else -> null
    }
  }

  private fun convertData(data: String): DATA {
    @Suppress("UNCHECKED_CAST")
    return when {
      dataType.acceptsString() -> data as DATA
      else -> dataConverter.convert(data.toResponseBody()) ?: conversionError(data, dataType)
    }
  }
}
