/*
 * Copyright (C) 2018 Square, Inc.
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

@file:JvmName("KotlinExtensions")

package retrofit2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

inline fun <reified T> Retrofit.create(): T = create(T::class.java)

suspend fun <T : Any> Call<T>.await(): T = awaitResult().getOrThrow()

@JvmName("awaitNullable")
suspend fun <T : Any> Call<T?>.await(): T? = awaitResult().getOrThrow()

suspend fun <T : Any> Call<T>.awaitResult(): Result<T> {
  val result = (this as Call<T?>).awaitResult()
  if (result.isSuccess && result.getOrNull() == null) {
    val invocation = request().tag(Invocation::class.java)!!
    val method = invocation.method()
    val e = KotlinNullPointerException(
        "Response from " +
            method.declaringClass.name +
            '.' +
            method.name +
            " was null but response body type was declared as non-null"
    )
    return Result.failure(e)
  }
  return result as Result<T>
}

@JvmName("awaitNullableResult")
suspend fun <T : Any> Call<T?>.awaitResult(): Result<T?> {
  return suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
      cancel()
    }
    enqueue(object : Callback<T?> {
      override fun onResponse(call: Call<T?>, response: Response<T?>) {
        if (response.isSuccessful) {
          continuation.resume(Result.success(response.body()))
        } else {
          continuation.resume(Result.failure(HttpException(response)))
        }
      }

      override fun onFailure(call: Call<T?>, t: Throwable) {
        continuation.resume(Result.failure(t))
      }
    })
  }
}

suspend fun <T : Any> Call<T>.awaitResponse(): Response<T> = awaitResponseResult().getOrThrow()

suspend fun <T : Any> Call<T>.awaitResponseResult(): Result<Response<T>> {
  return suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
      cancel()
    }
    enqueue(object : Callback<T> {
      override fun onResponse(call: Call<T>, response: Response<T>) {
        continuation.resume(Result.success(response))
      }

      override fun onFailure(call: Call<T>, t: Throwable) {
        continuation.resume(Result.failure(t))
      }
    })
  }
}

/**
 * Force the calling coroutine to suspend before throwing [this].
 *
 * This is needed when a checked exception is synchronously caught in a [java.lang.reflect.Proxy]
 * invocation to avoid being wrapped in [java.lang.reflect.UndeclaredThrowableException].
 *
 * The implementation is derived from:
 * https://github.com/Kotlin/kotlinx.coroutines/pull/1667#issuecomment-556106349
 */
internal suspend fun Exception.suspendAndThrow(): Nothing {
  suspendCoroutineUninterceptedOrReturn<Nothing> { continuation ->
    Dispatchers.Default.dispatch(continuation.context, Runnable {
      continuation.intercepted().resumeWithException(this@suspendAndThrow)
    })
    COROUTINE_SUSPENDED
  }
}
