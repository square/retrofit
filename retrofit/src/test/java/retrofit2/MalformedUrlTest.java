package retrofit2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public class MalformedUrlTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @PUT("user:email={email}/login")
    Call<ResponseBody> login(@Path(value = "email") String email);

    @GET("./{email}")
    Call<ResponseBody> loginRelative(@Path("email") String email);
  }

  @Test
  public void malformedUrlTest() throws Exception {
    Retrofit retrofit = new Retrofit.Builder().baseUrl(server.url("/")).build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().addHeader("Content-Type", "text/plain").setBody("Success"));

    Call<ResponseBody> callLogin = example.login("username");
    Response<ResponseBody> responseLogin = callLogin.execute();
    assertEquals("Success", responseLogin.body().string());
  }

  @Test
  public void relativePathTest() throws Exception {
    Retrofit retrofit = new Retrofit.Builder().baseUrl(server.url("/")).build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().addHeader("Content-Type", "text/plain").setBody("Success"));

    Call<ResponseBody> callLoginRel = example.loginRelative("username");
    try {
      Response<ResponseBody> responseLoginRel = callLoginRel.execute();
      assertEquals("Success", responseLoginRel.body().string());
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage("@Path parameters shouldn't perform path traversal ('.' or '..'): username");
    }
  }
}
