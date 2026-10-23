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

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.adapter.sse.EventSourceCallAdapterFactory
import retrofit2.adapter.sse.ServerSentEvent
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Streaming

class SseKtxFlowCallAdapterFactoryTest {

  @Rule
  @JvmField
  val server: MockWebServer = MockWebServer()

  data class EventData(
    val data: String,
  )

  interface Service {
    @Streaming
    @GET("/")
    fun sse(): Flow<ServerSentEvent<Long, String, EventData>>
  }

  private lateinit var service: Service

  @Before
  fun setUp() {
    val retrofit =
      Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(SseKtxFlowCallAdapterFactory)
        .addCallAdapterFactory(EventSourceCallAdapterFactory)
        .build()
    service = retrofit.create<Service>()
  }

  @Test
  fun simpleEvents() = runBlocking {
    val mockResponse = MockResponse()
      .setHeader("Content-Type", "text/event-stream")
      .setBody(
        """
        |id: 1
        |event: TYPE1
        |data: {"data":"foo"}
        |
        |id: 2
        |data: {"data":"bar"}
        |
        """.trimMargin(),
      )
    server.enqueue(mockResponse)

    var count = 0
    service.sse().collect { serverSentEvent ->
      when (++count) {
        1 -> {
          assertThat(serverSentEvent.id).isEqualTo(1)
          assertThat(serverSentEvent.type).isEqualTo("TYPE1")
          assertThat(serverSentEvent.data.data).isEqualTo("foo")
        }
        2 -> {
          assertThat(serverSentEvent.id).isEqualTo(2)
          assertThat(serverSentEvent.type).isEqualTo(null)
          assertThat(serverSentEvent.data.data).isEqualTo("bar")
        }
      }
    }
  }
}
