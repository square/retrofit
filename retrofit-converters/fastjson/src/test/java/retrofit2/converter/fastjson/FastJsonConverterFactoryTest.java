package retrofit2.converter.fastjson;

import com.alibaba.fastjson.annotation.JSONField;
import fastjson.FastJsonConverterFactory;
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

import static org.assertj.core.api.Assertions.assertThat;

public final class FastJsonConverterFactoryTest {
    private static class Bean {
        @JSONField(name="name")
        private String theName;

        Bean() {}

        Bean(String name) {
            theName = name;
        }

        public String getTheName() {
            return theName;
        }

        public void setTheName(String theName) {
            this.theName = theName;
        }
    }

    interface Service {
        @POST("/")
        Call<Bean> action(@Body Bean impl);
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
    public void testAction() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().setBody("{\"name\":\"value\"}"));

        Call<Bean> call = service.action(new Bean("value"));
        Response<Bean> response = call.execute();
        Bean body = response.body();
        assertThat(body.getTheName()).isEqualTo("value");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getBody().readUtf8()).isEqualTo("{\"name\":\"value\"}");
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
    }

    @Test public void testNullValueSerialization() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().setBody("{}"));

        service.action(new Bean(null)).execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getBody().readUtf8()).isEqualTo("{}"); // Null value was not serialized.
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
    }

    @Test public void testNullValueDeserialization() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().setBody("{/* a comment! */}"));

        Response<Bean> response =
                service.action(new Bean("value")).execute();
        assertThat(response.body().getTheName()).isNull();
    }
}
