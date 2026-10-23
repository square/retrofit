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
package retrofit2.adapter.sse.java9

import java.lang.reflect.Type
import java.util.concurrent.Executor
import java.util.concurrent.Flow
import java.util.concurrent.SubmissionPublisher
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.adapter.sse.EventSource
import retrofit2.adapter.sse.ServerSentEvent
import retrofit2.adapter.sse.SseCallback

internal class SseJucFlowCallAdapter<ID : Any, TYPE : Any, DATA : Any>(
  private val executor: Executor,
  private val maxBufferCapacity: Int,
  private val delegation: CallAdapter<ResponseBody, EventSource<ID, TYPE, DATA>>,
) : CallAdapter<ResponseBody, Flow.Publisher<ServerSentEvent<ID, TYPE, DATA>>> {

  override fun responseType(): Type = delegation.responseType()

  override fun adapt(
    call: Call<ResponseBody>,
  ): Flow.Publisher<ServerSentEvent<ID, TYPE, DATA>> {
    val delegate = delegation.adapt(call)
    return object : SubmissionPublisher<ServerSentEvent<ID, TYPE, DATA>>(executor, maxBufferCapacity) {
      override fun subscribe(subscriber: Flow.Subscriber<in ServerSentEvent<ID, TYPE, DATA>>?) {
        super.subscribe(subscriber)
        delegate.subscribe(object : SseCallback<ID, TYPE, DATA> {
          override fun onEvent(
            eventSource: EventSource<ID, TYPE, DATA>,
            id: ID?,
            type: TYPE?,
            data: DATA,
          ) {
            submit(ServerSentEvent(id, type, data))
          }

          override fun onClosed(eventSource: EventSource<ID, TYPE, DATA>) {
            close()
          }

          override fun onFailure(eventSource: EventSource<ID, TYPE, DATA>, t: Throwable?) {
            closeExceptionally(t ?: RuntimeException()) // TODO exception type
          }
        })
      }

      override fun close() {
        delegate.cancel()
        super.close()
      }
    }
  }

}
