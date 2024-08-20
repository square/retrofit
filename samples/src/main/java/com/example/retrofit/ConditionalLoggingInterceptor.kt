/*
 * Copyright (C) 2024 Square, Inc.
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
package com.example.retrofit

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Invocation
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET

suspend fun main() {
  val server = MockWebServer()
  val client = OkHttpClient.Builder()
    .addInterceptor(
      ConditionalLoggingInterceptor(
        HttpLoggingInterceptor(::println).setLevel(
          HttpLoggingInterceptor.Level.BODY,
        ),
      ),
    )
    .build()
  val retrofit = Retrofit.Builder()
    .baseUrl(server.url("/"))
    .client(client)
    .build()
  val exampleApi = retrofit.create<ExampleApi>()

  server.enqueue(MockResponse())
  exampleApi.one()

  server.enqueue(MockResponse())
  exampleApi.two()
}

private interface ExampleApi {
  @GET("one")
  suspend fun one(): ResponseBody

  @Log
  @GET("two")
  suspend fun two(): ResponseBody
}

/**
 * Retrofit service functions which are annotated with this class will have their HTTP calls
 * logged. You must add [ConditionalLoggingInterceptor] to your [OkHttpClient] for this to work.
 */
annotation class Log

class ConditionalLoggingInterceptor(
  private val loggingInterceptor: HttpLoggingInterceptor,
) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    request.tag(Invocation::class.java)?.let { invocation ->
      if (invocation.method().isAnnotationPresent(Log::class.java)) {
        return loggingInterceptor.intercept(chain)
      }
    }
    return chain.proceed(request)
  }
}
