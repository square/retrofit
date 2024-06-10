package retrofit2.converter.kotlinx.serialization.json

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import okhttp3.ResponseBody
import retrofit2.Converter

@ExperimentalSerializationApi
class ResponseBodyConverter<T>(
  private val json: Json,
  private val loader: DeserializationStrategy<T>,
) : Converter<ResponseBody, T> {

  override fun convert(value: ResponseBody): T? {
    val source = value.source()
    return json.decodeFromBufferedSource(loader, source)
  }
}
