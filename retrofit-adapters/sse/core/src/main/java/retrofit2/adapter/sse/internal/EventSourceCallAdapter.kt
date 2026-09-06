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

import java.lang.reflect.Type
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.sse.EventSource

private val EMPTY_ARRAY = emptyArray<Annotation>()

class EventSourceCallAdapter<ID : Any, TYPE : Any, DATA : Any>(
  retrofit: Retrofit,
  private val idType: Type,
  private val typeType: Type,
  private val dataType: Type,
) : CallAdapter<ResponseBody, EventSource<ID, TYPE, DATA>> {

  private val idConverter: Converter<ResponseBody, ID> = retrofit.responseBodyConverter(idType, EMPTY_ARRAY)
  private val typeConverter: Converter<ResponseBody, TYPE> = retrofit.responseBodyConverter(typeType, EMPTY_ARRAY)
  private val dataConverter: Converter<ResponseBody, DATA> = retrofit.responseBodyConverter(dataType, EMPTY_ARRAY)

  override fun responseType(): Type = ResponseBody::class.java

  override fun adapt(call: Call<ResponseBody>): EventSource<ID, TYPE, DATA> {
    return RealEventSource(idType, typeType, dataType, idConverter, typeConverter, dataConverter, call)
  }
}
