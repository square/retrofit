package retrofit2.converter.kotlinx.serialization.json

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.converter.kotlinx.serialization.Serializer

@ExperimentalSerializationApi
class SerializerFromJson(override val format: Json) : Serializer() {
  override fun <T> fromResponseBody(loader: DeserializationStrategy<T>, body: ResponseBody): T {
    val source = body.source()
    return format.decodeFromBufferedSource(loader, source)
  }

  override fun <T> toRequestBody(contentType: MediaType, saver: SerializationStrategy<T>, value: T): RequestBody {
    val string = format.encodeToString(saver, value)
    return RequestBody.create(contentType, string)
  }
}
