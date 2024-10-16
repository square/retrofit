package retrofit2.converter.kotlinx.serialization.json

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Converter

internal class RequestBodyConverter<T>(
  private val json: Json,
  private val saver: SerializationStrategy<T>,
) : Converter<T, RequestBody> {

  private val contentType = MediaType.get("application/json; charset=UTF-8")

  override fun convert(value: T): RequestBody {
    val string = json.encodeToString(saver, value)
    return RequestBody.create(contentType, string)
  }
}
