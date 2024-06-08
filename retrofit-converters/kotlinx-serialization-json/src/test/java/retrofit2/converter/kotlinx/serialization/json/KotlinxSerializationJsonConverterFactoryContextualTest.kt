package retrofit2.converter.kotlinx.serialization.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
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

class KotlinxSerializationJsonConverterFactoryContextualTest {
  @get:Rule
  val server = MockWebServer()

  private lateinit var service: Service

  interface Service {
    @GET("/")
    fun deserialize(): Call<User>

    @POST("/")
    fun serialize(@Body user: User): Call<Void?>
  }

  data class User(val name: String)

  object UserSerializer : KSerializer<User> {
    override val descriptor = PrimitiveSerialDescriptor("User", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): User =
      decoder.decodeSerializableValue(UserResponse.serializer()).run {
        User(name)
      }

    override fun serialize(encoder: Encoder, value: User): Unit =
      encoder.encodeSerializableValue(UserResponse.serializer(), UserResponse(value.name))

    @Serializable
    private data class UserResponse(val name: String)
  }

  private val json = Json {
    serializersModule = SerializersModule {
      contextual(UserSerializer)
    }
  }

  @Before
  fun setUp() {
    val retrofit = Retrofit.Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(json.asConverterFactory())
      .build()
    service = retrofit.create(Service::class.java)
  }

  @Test
  fun deserialize() {
    server.enqueue(MockResponse().setBody("""{"name":"Bob"}"""))
    val user = service.deserialize().execute().body()!!
    assertEquals(User("Bob"), user)
  }

  @Test
  fun serialize() {
    server.enqueue(MockResponse())
    service.serialize(User("Bob")).execute()
    val request = server.takeRequest()
    assertEquals("""{"name":"Bob"}""", request.body.readUtf8())
    assertEquals("application/json; charset=UTF-8", request.headers["Content-Type"])
  }
}
