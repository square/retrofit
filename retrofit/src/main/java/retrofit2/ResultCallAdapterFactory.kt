package retrofit2

import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import okhttp3.Request
import okio.Timeout

class ResultCallAdapterFactory private constructor() : CallAdapter.Factory() {
  override fun get(
    returnType: Type,
    annotations: Array<Annotation>,
    retrofit: Retrofit,
  ): CallAdapter<*, *>? {
    if (getRawType(returnType) != Result::class.java) return null

    check(returnType is ParameterizedType) {
      "Result must have a generic type (e.g., Result<T>)"
    }

    return ResultCallAdapter<Any>(getParameterUpperBound(0, returnType))
  }

  companion object {
    @JvmStatic
    fun create(): CallAdapter.Factory = ResultCallAdapterFactory()
  }
}

class ResultCallAdapter<T>(
  private val responseType: Type,
) : CallAdapter<T, Call<Result<T>>> {

  override fun responseType(): Type = responseType

  override fun adapt(call: Call<T>): Call<Result<T>> = ResultCall(call)
}

class ResultCall<T>(private val delegate: Call<T>) : Call<Result<T>> {

  override fun enqueue(callback: Callback<Result<T>>) {
    delegate.enqueue(object : Callback<T> {
      override fun onResponse(call: Call<T>, response: Response<T>) {
        val result = runCatching {
          if (response.isSuccessful) {
            response.body() ?: error("Response $response body is null.")
          } else {
            throw HttpException(response)
          }
        }
        callback.onResponse(this@ResultCall, Response.success(result))
      }

      override fun onFailure(call: Call<T>, t: Throwable) {
        callback.onResponse(this@ResultCall, Response.success(Result.failure(t)))
      }
    })
  }

  override fun execute(): Response<Result<T>> {
    val result = runCatching {
      val response = delegate.execute()
      if (response.isSuccessful) {
        response.body() ?: error("Response $response body is null.")
      } else {
        throw IOException("Unexpected error: ${response.errorBody()?.string()}")
      }
    }
    return Response.success(result)
  }

  override fun isExecuted(): Boolean = delegate.isExecuted

  override fun clone(): ResultCall<T> = ResultCall(delegate.clone())

  override fun isCanceled(): Boolean = delegate.isCanceled

  override fun cancel(): Unit = delegate.cancel()

  override fun request(): Request = delegate.request()

  override fun timeout(): Timeout = delegate.timeout()
}
