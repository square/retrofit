package com.jakewharton.retrofit2.converter.kotlinx.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

class KotlinSerializationConverterFactoryStringTest {
  @get:Rule val server = MockWebServer()

  private lateinit var service: Service

  interface Service {
    @GET("/") fun deserialize(): Call<User>
    @POST("/") fun serialize(@Body user: User): Call<Void?>
  }

  @Serializable
  data class User(val name: String)

  @Before fun setUp() {
    val contentType = MediaType.get("application/json; charset=utf-8")
    val retrofit = Retrofit.Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(Json.asConverterFactory(contentType))
      .build()
    service = retrofit.create(Service::class.java)
  }

  @Test fun deserialize() {
    server.enqueue(MockResponse().setBody("""{"name":"Bob"}"""))
    val user = service.deserialize().execute().body()!!
    assertEquals(User("Bob"), user)
  }

  @Test fun serialize() {
    server.enqueue(MockResponse())
    service.serialize(User("Bob")).execute()
    val request = server.takeRequest()
    assertEquals("""{"name":"Bob"}""", request.body.readUtf8())
    assertEquals("application/json; charset=utf-8", request.headers["Content-Type"])
  }
}
