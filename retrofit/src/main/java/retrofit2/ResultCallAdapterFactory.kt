package retrofit2

import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import okhttp3.Request
import okio.Timeout

class ResultCallAdapterFactory : CallAdapter.Factory() {
  override fun get(
    returnType: Type,
    annotations: Array<Annotation>,
    retrofit: Retrofit
  ): CallAdapter<*, *>? {
    if (getRawType(returnType) != Result::class.java) {
      return null
    }

    check(returnType is ParameterizedType) {
      "Result must have a generic type (e.g., Result<T>)"
    }

    val responseType = getParameterUpperBound(0, returnType)
    return ResultCallAdapter<Any>(responseType)
  }
}

class ResultCallAdapter<T>(
  private val responseType: Type
) : CallAdapter<T, Call<Result<T>>> {

  override fun responseType(): Type {
    return responseType
  }

  override fun adapt(call: Call<T>): Call<Result<out T>> {
    return ResultCall(call)
  }
}

class ResultCall<T>(private val delegate: Call<T>) : Call<Result<out T>> {

  override fun enqueue(callback: Callback<Result<T>>) {
    delegate.enqueue(object : Callback<T> {
      override fun onResponse(call: Call<T>, response: Response<T>) {
        val result = if (response.isSuccessful) {
          Result.success(response.body()!!)
        } else {
          Result.failure(HttpException(response))
        }
        callback.onResponse(this@ResultCall, Response.success(result))
      }

      override fun onFailure(call: Call<T>, t: Throwable) {
        callback.onResponse(this@ResultCall, Response.success(Result.failure(t)))
      }
    })
  }

  override fun execute(): Response<Result<T>> {
    return try {
      val response = delegate.execute()
      val result = if (response.isSuccessful) {
        Result.success(response.body()!!)
      } else {
        Result.failure(IOException("Unexpected error: ${response.errorBody()?.string()}"))
      }
      Response.success(result)
    } catch (e: IOException) {
      Response.success(Result.failure(e))
    } catch (e: Exception) {
      Response.success(Result.failure(e))
    }
  }
  override fun isExecuted(): Boolean = delegate.isExecuted

  override fun clone(): ResultCall<T> = ResultCall(delegate.clone())

  override fun isCanceled(): Boolean = delegate.isCanceled

  override fun cancel() = delegate.cancel()

  override fun request(): Request = delegate.request()

  override fun timeout(): Timeout = delegate.timeout()
}


