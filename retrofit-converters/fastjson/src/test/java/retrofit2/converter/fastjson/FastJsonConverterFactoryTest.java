package retrofit2.converter.fastjson;


import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.POST;

import java.io.IOException;
import java.lang.reflect.Type;

import static org.assertj.core.api.Assertions.assertThat;

public class FastJsonConverterFactoryTest {
  interface AnInterface {
    @JSONField(name = "name")
    String getName();
    @JSONField(name = "name")
    void setName(String name);
  }

  static class AnImplementation implements AnInterface {

    @JSONField(name = "name")
    private String theName;

    AnImplementation() {
    }

    AnImplementation(String name) {
      theName = name;
    }

    @JSONField(name = "name")
    @Override public String getName() {
      return theName;
    }

    @JSONField(name = "name")
    @Override
    public void setName(String name) {
      theName = name;
    }
  }

  interface Service {
    @POST("/")
    Call<AnImplementation> anImplementation(@Body AnImplementation impl);
    @POST("/") Call<AnInterface> anInterface(@Body AnInterface impl);
  }

  @Rule
  public final MockWebServer server = new MockWebServer();

  private Service service;

  @Before
  public void setUp() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(FastJsonConverterFactory.create())
        .build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void anInterface() throws IOException, InterruptedException {
    server.enqueue(new MockResponse().setBody("{\"name\":\"value\"}"));

    Call<AnInterface> call = service.anInterface(new AnImplementation("value"));
    Response<AnInterface> response = call.execute();
    AnInterface body = response.body();
    assertThat(body.getName()).isEqualTo("value");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readUtf8()).isEqualTo("{\"name\":\"value\"}");
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
  }

  @Test public void anImplementation() throws IOException, InterruptedException {
    server.enqueue(new MockResponse().setBody("{\"theName\":\"value\"}"));

    Call<AnImplementation> call = service.anImplementation(new AnImplementation("value"));
    Response<AnImplementation> response = call.execute();
    AnImplementation body = response.body();
    assertThat(body.theName).isNull();

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readUtf8()).isEqualTo("{\"name\":\"value\"}");
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
  }

  @Test public void serializeUsesConfiguration() throws IOException, InterruptedException {
    server.enqueue(new MockResponse().setBody("{}"));

    service.anImplementation(new AnImplementation(null)).execute();

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readUtf8()).isEqualTo("{}"); // Null value was not serialized.
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
  }

}
