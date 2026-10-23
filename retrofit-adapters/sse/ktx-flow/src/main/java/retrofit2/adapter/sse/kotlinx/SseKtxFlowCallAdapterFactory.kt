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
package retrofit2.adapter.sse.kotlinx

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlinx.coroutines.flow.Flow
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.sse.EventSource
import retrofit2.adapter.sse.ServerSentEvent
import retrofit2.adapter.sse.internal.EventSourceCallAdapter
import retrofit2.http.GET
import retrofit2.http.Streaming

object SseKtxFlowCallAdapterFactory : CallAdapter.Factory() {

  override fun get(
    returnType: Type,
    annotations: Array<out Annotation?>,
    retrofit: Retrofit,
  ): CallAdapter<*, *>? {
    if (getRawType(returnType) != Flow::class.java) {
      return null
    }

    if (returnType !is ParameterizedType) {
      error(
        "Flow return type must be parameterized as Flow<Foo> or Flow<? extends Foo>",
      )
    }

    val innerType = getParameterUpperBound(0, returnType)

    if (getRawType(innerType) != ServerSentEvent::class.java) {
      return null
    }

    if (innerType !is ParameterizedType) {
      error(
        "ServerSentEvent must be parameterized as ServerSentEvent<ID, TYPE, DATA>" +
          " or ServerSentEvent<? extends ID, ? extends TYPE, ? extends DATA>",
      )
    }

    if (annotations.none { it is Streaming }) {
      error("SSE endpoint must be annotated with @Streaming")
    }

    if (annotations.none { it is GET }) {
      error("SSE endpoint must use @GET method")
    }

    val idType = getParameterUpperBound(0, innerType)
    val typeType = getParameterUpperBound(1, innerType)
    val dataType = getParameterUpperBound(2, innerType)

    val returnType = object : ParameterizedType {
      override fun getRawType(): Type = EventSource::class.java
      override fun getOwnerType(): Type? = null
      override fun getActualTypeArguments(): Array<Type> = arrayOf(idType, typeType, dataType)
    }

    val delegation = runCatching {
      retrofit.callAdapter(returnType, annotations) as? EventSourceCallAdapter<*, *, *>
    }.getOrNull() ?: return null

    return SseKtxFlowCallAdapter(delegation)
  }
}
