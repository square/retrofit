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

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import retrofit2.helpers.ToStringConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.IOException

class KotlinSuspendTest {
  @get:Rule val server = MockWebServer()

  interface Service {
    @GET("/") suspend fun body(): String
    @GET("/") suspend fun response(): Response<String>

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
      // TODO
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
      // TODO
    }
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
      // TODO
    }
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
}
