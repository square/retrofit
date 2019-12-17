/*
 * Copyright (C) 2019 Square, Inc.
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
package retrofit2.mock

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.util.Random
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.NANOSECONDS

class BehaviorDelegateKotlinTest {
  internal interface DoWorkService {
    suspend fun body(): String
    suspend fun failure(): String
    suspend fun response(): Response<String>
    suspend fun responseWildcard(): Response<out String>
  }

  private val mockFailure = IOException("Timeout!")
  private val behavior = NetworkBehavior.create(Random(2847))
  private lateinit var service: DoWorkService

  @Before fun before() {
    val retrofit = Retrofit.Builder()
        .baseUrl("http://example.com")
        .build()
    val mockRetrofit = MockRetrofit.Builder(retrofit)
        .networkBehavior(behavior)
        .build()
    val delegate = mockRetrofit.create<DoWorkService>()

    service = object : DoWorkService {
      override suspend fun body(): String {
        return delegate.returning(Calls.response("Response!")).body()
      }

      override suspend fun failure(): String {
        val failure = Calls.failure<String>(mockFailure)
        return delegate.returning(failure).failure()
      }

      override suspend fun response(): Response<String> {
        val response = Calls.response("Response!")
        return delegate.returning(response).response()
      }

      override suspend fun responseWildcard() = response()
    }
  }

  @Test fun body() {
    behavior.setDelay(100, MILLISECONDS)
    behavior.setVariancePercent(0)
    behavior.setFailurePercent(0)

    val startNanos = System.nanoTime()
    val result = runBlocking { service.body() }
    val tookMs = NANOSECONDS.toMillis(System.nanoTime() - startNanos)

    assertThat(tookMs).isGreaterThanOrEqualTo(100)
    assertThat(result).isEqualTo("Response!")
  }

  @Test fun bodyFailure() {
    behavior.setDelay(100, MILLISECONDS)
    behavior.setVariancePercent(0)
    behavior.setFailurePercent(100)

    val startNanos = System.nanoTime()
    val exception = runBlocking {
      try {
        throw AssertionError(service.body())
      } catch (e: Exception) {
        e
      }
    }
    val tookMs = NANOSECONDS.toMillis(System.nanoTime() - startNanos)

    assertThat(tookMs).isGreaterThanOrEqualTo(100)
    assertThat(exception).isSameAs(behavior.failureException())
  }

  @Test fun failure() {
    behavior.setDelay(100, MILLISECONDS)
    behavior.setVariancePercent(0)
    behavior.setFailurePercent(0)

    val startNanos = System.nanoTime()
    val exception = runBlocking {
      try {
        throw AssertionError(service.failure())
      } catch (e: Exception) {
        e
      }
    }
    val tookMs = NANOSECONDS.toMillis(System.nanoTime() - startNanos)

    assertThat(tookMs).isGreaterThanOrEqualTo(100)
    // Coroutines break referential transparency on exceptions so compare type and message.
    assertThat(exception).isExactlyInstanceOf(mockFailure.javaClass)
    assertThat(exception).hasMessage(mockFailure.message)
  }

  @Test fun response() {
    behavior.setDelay(100, MILLISECONDS)
    behavior.setVariancePercent(0)
    behavior.setFailurePercent(0)

    val startNanos = System.nanoTime()
    val result = runBlocking { service.response() }
    val tookMs = NANOSECONDS.toMillis(System.nanoTime() - startNanos)

    assertThat(tookMs).isGreaterThanOrEqualTo(100)
    assertThat(result.body()).isEqualTo("Response!")
  }

  @Test fun responseFailure() {
    behavior.setDelay(100, MILLISECONDS)
    behavior.setVariancePercent(0)
    behavior.setFailurePercent(100)

    val startNanos = System.nanoTime()
    val exception = runBlocking {
      try {
        throw AssertionError(service.response())
      } catch (e: Exception) {
        e
      }
    }
    val tookMs = NANOSECONDS.toMillis(System.nanoTime() - startNanos)

    assertThat(tookMs).isGreaterThanOrEqualTo(100)
    assertThat(exception).isSameAs(behavior.failureException())
  }

  @Test fun responseWildcard() {
    behavior.setDelay(100, MILLISECONDS)
    behavior.setVariancePercent(0)
    behavior.setFailurePercent(0)

    val startNanos = System.nanoTime()
    val result = runBlocking { service.responseWildcard() }
    val tookMs = NANOSECONDS.toMillis(System.nanoTime() - startNanos)

    assertThat(tookMs).isGreaterThanOrEqualTo(100)
    assertThat(result.body()).isEqualTo("Response!")
  }
}
