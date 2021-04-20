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
import retrofit2.kotlinx.metadata.Flag
import retrofit2.kotlinx.metadata.KmClassifier
import retrofit2.kotlinx.metadata.jvm.KotlinClassHeader
import retrofit2.kotlinx.metadata.jvm.KotlinClassMetadata
import retrofit2.kotlinx.metadata.jvm.signature
import java.lang.reflect.Method
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

inline fun <reified T> Retrofit.create(): T = create(T::class.java)

suspend fun <T : Any> Call<T>.await(): T {
  return suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
      cancel()
    }
    enqueue(object : Callback<T> {
      override fun onResponse(call: Call<T>, response: Response<T>) {
        if (response.isSuccessful) {
          val body = response.body()
          if (body == null) {
            val invocation = call.request().tag(Invocation::class.java)!!
            val method = invocation.method()
            val e = KotlinNullPointerException("Response from " +
                method.declaringClass.name +
                '.' +
                method.name +
                " was null but response body type was declared as non-null")
            continuation.resumeWithException(e)
          } else {
            continuation.resume(body)
          }
        } else {
          continuation.resumeWithException(HttpException(response))
        }
      }

      override fun onFailure(call: Call<T>, t: Throwable) {
        continuation.resumeWithException(t)
      }
    })
  }
}

@JvmName("awaitNullable")
suspend fun <T : Any> Call<T?>.await(): T? {
  return suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
      cancel()
    }
    enqueue(object : Callback<T?> {
      override fun onResponse(call: Call<T?>, response: Response<T?>) {
        if (response.isSuccessful) {
          continuation.resume(response.body())
        } else {
          continuation.resumeWithException(HttpException(response))
        }
      }

      override fun onFailure(call: Call<T?>, t: Throwable) {
        continuation.resumeWithException(t)
      }
    })
  }
}

suspend fun <T> Call<T>.awaitResponse(): Response<T> {
  return suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
      cancel()
    }
    enqueue(object : Callback<T> {
      override fun onResponse(call: Call<T>, response: Response<T>) {
        continuation.resume(response)
      }

      override fun onFailure(call: Call<T>, t: Throwable) {
        continuation.resumeWithException(t)
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

internal fun isReturnTypeNullable(method: Method): Boolean {
  val declaringClass = method.declaringClass
  val metadataAnnotation = declaringClass.getAnnotation(Metadata::class.java)

  val header = KotlinClassHeader(
    kind = metadataAnnotation.kind,
    metadataVersion = metadataAnnotation.metadataVersion,
    data1 = metadataAnnotation.data1,
    data2 = metadataAnnotation.data2,
    extraString = metadataAnnotation.extraString,
    extraInt = metadataAnnotation.extraInt,
    packageName = metadataAnnotation.packageName
  )

  val classMetadata = KotlinClassMetadata.read(header)
  val kmClass = (classMetadata as KotlinClassMetadata.Class).toKmClass()

  val javaMethodSignature = method.createSignature()
  val candidates = kmClass.functions.filter { it.signature?.asString() == javaMethodSignature }

  require(candidates.isNotEmpty()) { "No match found in metadata for '${method}'" }
  require(candidates.size == 1) { "Multiple function matches found in metadata for '${method}'" }
  val match = candidates.first()

  return Flag.Type.IS_NULLABLE(match.returnType.flags) || match.returnType.classifier == KmClassifier.Class("kotlin/Unit")
}

private fun Method.createSignature() = buildString {
  append(name)
  append('(')

  parameterTypes.forEach {
    append(it.typeToSignature())
  }

  append(')')

  append(returnType.typeToSignature())
}

private fun Class<*>.typeToSignature() = when {
  isPrimitive -> javaTypesMap[name]
  isArray -> name.replace('.', '/')
  else -> "L${name.replace('.', '/')};"
}

private val javaTypesMap = mapOf(
  "int" to "I",
  "long" to "J",
  "boolean" to "Z",
  "byte" to "B",
  "char" to "C",
  "float" to "F",
  "double" to "D",
  "short" to "S",
  "void" to "V"
)
