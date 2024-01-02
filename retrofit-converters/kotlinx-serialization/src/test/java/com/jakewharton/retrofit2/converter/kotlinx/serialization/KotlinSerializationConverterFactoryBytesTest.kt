package com.jakewharton.retrofit2.converter.kotlinx.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import okhttp3.MediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.ByteString
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

private val bobBytes = ByteString.of(0x0a, 0x03, 'B'.code.toByte(), 'o'.code.toByte(), 'b'.code.toByte())

@ExperimentalSerializationApi
class KotlinSerializationConverterFactoryBytesTest {
  @get:Rule val server = MockWebServer()

  private lateinit var service: Service

  interface Service {
    @GET("/") fun deserialize(): Call<User>
    @POST("/") fun serialize(@Body user: User): Call<Void?>
  }

  @Serializable
  data class User(@ProtoNumber(1) val name: String)

  @Before fun setUp() {
    val contentType = MediaType.get("application/x-protobuf")
    val retrofit = Retrofit.Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(ProtoBuf.asConverterFactory(contentType))
      .build()
    service = retrofit.create(Service::class.java)
  }

  @Test fun deserialize() {
    server.enqueue(MockResponse().setBody(Buffer().write(bobBytes)))
    val user = service.deserialize().execute().body()!!
    assertEquals(User("Bob"), user)
  }

  @Test fun serialize() {
    server.enqueue(MockResponse())
    service.serialize(User("Bob")).execute()
    val request = server.takeRequest()
    assertEquals(bobBytes, request.body.readByteString())
    assertEquals("application/x-protobuf", request.headers["Content-Type"])
  }
}
