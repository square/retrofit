/*
 * Copyright (C) 2016 Square, Inc.
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
package retrofit.adapter.kotlin.coroutines.experimental

import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * A [CallAdapter.Factory] for use with Kotlin coroutines.
 *
 * Adding this class to [Retrofit] allows you to return [Deferred] from
 * service methods.
 *
 *     interface MyService {
 *       &#64;GET("user/me")
 *       Deferred&lt;User&gt; getUser()
 *     }
 *
 * There are two configurations supported for the [Deferred] type parameter:
 *
 * * Direct body (e.g., `Deferred<User>`) returns the deserialized body for 2XX responses, throws
 * [HttpException] errors for non-2XX responses, and throws [IOException][java.io.IOException] for
 * network errors.
 * * Response wrapped body (e.g., `Deferred<Response<User>>`) returns a [Response] object for all
 * HTTP responses and throws [IOException][java.io.IOException] for network errors
 */
class CoroutineCallAdapterFactory private constructor() : CallAdapter.Factory() {
  companion object {
    @JvmStatic @JvmName("create")
    operator fun invoke() = CoroutineCallAdapterFactory()
  }

  override fun get(
      returnType: Type,
      annotations: Array<out Annotation>,
      retrofit: Retrofit
  ): CallAdapter<*, *>? {
    if (Deferred::class.java != getRawType(returnType)) {
      return null
    }
    if (returnType !is ParameterizedType) {
      throw IllegalStateException(
          "Deferred return type must be parameterized as Deferred<Foo> or Deferred<out Foo>")
    }
    val responseType = getParameterUpperBound(0, returnType)

    val rawDeferredType = getRawType(responseType)
    return if (rawDeferredType == Response::class.java) {
      if (responseType !is ParameterizedType) {
        throw IllegalStateException(
            "Response must be parameterized as Response<Foo> or Response<out Foo>")
      }
      ResponseCallAdapter<Any>(getParameterUpperBound(0, responseType))
    } else {
      BodyCallAdapter<Any>(responseType)
    }
  }

  private class BodyCallAdapter<T>(
      private val responseType: Type
  ) : CallAdapter<T, Deferred<T>> {

    override fun responseType() = responseType

    override fun adapt(call: Call<T>): Deferred<T> {
      val deferred = CompletableDeferred<T>()

      deferred.invokeOnCompletion {
        if (deferred.isCancelled) {
          call.cancel()
        }
      }

      call.enqueue(object : Callback<T> {
        override fun onFailure(call: Call<T>, t: Throwable) {
          deferred.completeExceptionally(t)
        }

        override fun onResponse(call: Call<T>, response: Response<T>) {
          if (response.isSuccessful) {
            deferred.complete(response.body()!!)
          } else {
            deferred.completeExceptionally(HttpException(response))
          }
        }
      })

      return deferred
    }
  }

  private class ResponseCallAdapter<T>(
      private val responseType: Type
  ) : CallAdapter<T, Deferred<Response<T>>> {

    override fun responseType() = responseType

    override fun adapt(call: Call<T>): Deferred<Response<T>> {
      val deferred = CompletableDeferred<Response<T>>()

      deferred.invokeOnCompletion {
        if (deferred.isCancelled) {
          call.cancel()
        }
      }

      call.enqueue(object : Callback<T> {
        override fun onFailure(call: Call<T>, t: Throwable) {
          deferred.completeExceptionally(t)
        }

        override fun onResponse(call: Call<T>, response: Response<T>) {
          deferred.complete(response)
        }
      })

      return deferred
    }
  }
}
