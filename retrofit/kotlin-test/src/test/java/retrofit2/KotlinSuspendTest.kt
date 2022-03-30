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
package retrofit2

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST
import okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import retrofit2.helpers.ToStringConverterFactory
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Path
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.coroutines.CoroutineContext

class KotlinSuspendTest {
  @get:Rule val server = MockWebServer()

  interface Service {
    @GET("/") suspend fun body(): String
    @GET("/") suspend fun bodyNullable(): String?
    @GET("/") suspend fun response(): Response<String>
    @GET("/") suspend fun unit()
    @HEAD("/") suspend fun headUnit()

    @GET("/{a}/{b}/{c}")
    suspend fun params(
        @Path("a") a: String,
        @Path("b") b: String,
        @Path("c") c: String
    ): String
  }

  @Test fun body() {
    val retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(ToStringConverterFactory())
        .build()
    val example = retrofit.create(Service::class.java)

    server.enqueue(MockResponse().setBody("Hi"))

    val body = runBlocking { example.body() }
    assertThat(body).isEqualTo("Hi")
  }

  @Test fun body404() {
    val retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(ToStringConverterFactory())
        .build()
    val example = retrofit.create(Service::class.java)

    server.enqueue(MockResponse().setResponseCode(404))

    try {
      runBlocking { example.body() }
      fail()
    } catch (e: HttpException) {
      assertThat(e.code()).isEqualTo(404)
    }
  }

  @Test fun bodyFailure() {
    val retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(ToStringConverterFactory())
        .build()
    val example = retrofit.create(Service::class.java)

    server.enqueue(MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST))

    try {
      runBlocking { example.body() }
      fail()
    } catch (e: IOException) {
    }
  }

  @Test fun bodyThrowsOnNull() {
    val retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(ToStringConverterFactory())
        .build()
    val example = retrofit.create(Service::class.java)

    server.enqueue(MockResponse().setResponseCode(204))

    try {
      runBlocking { example.body() }
      fail()
    } catch (e: KotlinNullPointerException) {
      // Coroutines wraps exceptions with a synthetic trace so fall back to cause message.
      val message = e.message ?: (e.cause as KotlinNullPointerException).message
      assertThat(message).isEqualTo(
          "Response from retrofit2.KotlinSuspendTest\$Service.body was null but response body type was declared as non-null")
    }
  }

  @Ignore("Not working yet")
  @Test fun bodyNullable() {
    val retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(ToStringConverterFactory())
        .build()
    val example = retrofit.create(Service::class.java)

    server.enqueue(MockResponse().setResponseCode(204))

    val body = runBlocking { example.bodyNullable() }
    assertThat(body).isNull()
  }

  @Test fun response() {
    val retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(ToStringConverterFactory())
        .build()
    val example = retrofit.create(Service::class.java)

    server.enqueue(MockResponse().setBody("Hi"))

    val response = runBlocking { example.response() }
    assertThat(response.code()).isEqualTo(200)
    assertThat(response.body()).isEqualTo("Hi")
  }

  @Test fun response404() {
    val retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(ToStringConverterFactory())
        .build()
    val example = retrofit.create(Service::class.java)

    server.enqueue(MockResponse().setResponseCode(404))

    val response = runBlocking { example.response() }
    assertThat(response.code()).isEqualTo(404)
  }

  @Test fun responseFailure() {
    val retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(ToStringConverterFactory())
        .build()
    val example = retrofit.create(Service::class.java)

    server.enqueue(MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST))

    try {
      runBlocking { example.response() }
      fail()
    } catch (e: IOException) {
    }
  }

  @Test fun unit() {
    val retrofit = Retrofit.Builder().baseUrl(server.url("/")).build()
    val example = retrofit.create(Service::class.java)
    server.enqueue(MockResponse().setBody("Unit"))
    runBlocking { example.unit() }
  }

  @Test fun unitNullableBody() {
    val retrofit = Retrofit.Builder().baseUrl(server.url("/")).build()
    val example = retrofit.create(Service::class.java)
    server.enqueue(MockResponse().setResponseCode(204))
    runBlocking { example.unit() }
  }

  @Test fun headUnit() {
    val retrofit = Retrofit.Builder().baseUrl(server.url("/")).build()
    val example = retrofit.create(Service::class.java)
    server.enqueue(MockResponse())
    runBlocking { example.headUnit() }
  }

  @Test fun params() {
    val retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(ToStringConverterFactory())
        .build()
    val example = retrofit.create(Service::class.java)

    server.enqueue(MockResponse())

    runBlocking { example.params("1", "2", "3") }
    val request = server.takeRequest()
    assertThat(request.path).isEqualTo("/1/2/3")
  }

  @Test fun cancelationWorks() {
    lateinit var call: okhttp3.Call

    val okHttpClient = OkHttpClient()
    val retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .callFactory {
          val newCall = okHttpClient.newCall(it)
          call = newCall
          newCall
        }
        .addConverterFactory(ToStringConverterFactory())
        .build()
    val example = retrofit.create(Service::class.java)

    // This leaves the connection open indefinitely allowing us to cancel without racing a body.
    server.enqueue(MockResponse().setSocketPolicy(NO_RESPONSE))

    val deferred = GlobalScope.async { example.body() }

    // This will block until the server has received the request ensuring it's in flight.
    server.takeRequest()

    deferred.cancel()
    assertTrue(call.isCanceled)
  }

  @Test fun doesNotUseCallbackExecutor() {
    val retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .callbackExecutor { fail() }
        .addConverterFactory(ToStringConverterFactory())
        .build()
    val example = retrofit.create(Service::class.java)

    server.enqueue(MockResponse().setBody("Hi"))

    val body = runBlocking { example.body() }
    assertThat(body).isEqualTo("Hi")
  }

  @Test fun usesCallAdapterForCall() {
    val callAdapterFactory = object : CallAdapter.Factory() {
      override fun get(returnType: Type, annotations: Array<Annotation>,
          retrofit: Retrofit): CallAdapter<*, *>? {
        if (getRawType(returnType) != Call::class.java) {
          return null
        }
        if (getParameterUpperBound(0, returnType as ParameterizedType) != String::class.java) {
          return null
        }
        return object : CallAdapter<String, Call<String>> {
          override fun responseType() = String::class.java
          override fun adapt(call: Call<String>): Call<String> {
            return object : Call<String> by call {
              override fun enqueue(callback: Callback<String>) {
                call.enqueue(object : Callback<String> by callback {
                  override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                      callback.onResponse(call, Response.success(response.body()?.repeat(5)))
                    } else {
                      callback.onResponse(call, response)
                    }
                  }
                })
              }
            }
          }
        }
      }
    }

    val retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addCallAdapterFactory(callAdapterFactory)
        .addConverterFactory(ToStringConverterFactory())
        .build()
    val example = retrofit.create(Service::class.java)

    server.enqueue(MockResponse().setBody("Hi"))

    val body = runBlocking { example.body() }
    assertThat(body).isEqualTo("HiHiHiHiHi")
  }

  @Test fun checkedExceptionsAreNotSynchronouslyThrownForBody() = runBlocking {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://unresolved-host.com/")
        .addConverterFactory(ToStringConverterFactory())
        .build()
    val example = retrofit.create(Service::class.java)

    server.shutdown()

    // Run with a dispatcher that prevents yield from actually deferring work. An old workaround
    // for this problem relied on yield, but it is not guaranteed to prevent direct execution.
    withContext(DirectUnconfinedDispatcher) {
      // The problematic behavior of the UnknownHostException being synchronously thrown is
      // probabilistic based on thread preemption. Running a thousand times will almost always
      // trigger it, so we run an order of magnitude more to be safe.
      repeat(10000) {
        try {
          example.body()
          fail()
        } catch (_: IOException) {
          // We expect IOException, the bad behavior will wrap this in UndeclaredThrowableException.
        }
      }
    }
  }

  @Test fun checkedExceptionsAreNotSynchronouslyThrownForResponse() = runBlocking {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://unresolved-host.com/")
        .addConverterFactory(ToStringConverterFactory())
        .build()
    val example = retrofit.create(Service::class.java)

    server.shutdown()

    // Run with a dispatcher that prevents yield from actually deferring work. An old workaround
    // for this problem relied on yield, but it is not guaranteed to prevent direct execution.
    withContext(DirectUnconfinedDispatcher) {
      // The problematic behavior of the UnknownHostException being synchronously thrown is
      // probabilistic based on thread preemption. Running a thousand times will almost always
      // trigger it, so we run an order of magnitude more to be safe.
      repeat(10000) {
        try {
          example.response()
          fail()
        } catch (_: IOException) {
          // We expect IOException, the bad behavior will wrap this in UndeclaredThrowableException.
        }
      }
    }
  }

  @Suppress("EXPERIMENTAL_OVERRIDE")
  private object DirectUnconfinedDispatcher : CoroutineDispatcher() {
    override fun isDispatchNeeded(context: CoroutineContext): Boolean = false
    override fun dispatch(context: CoroutineContext, block: Runnable) = block.run()
  }
}
