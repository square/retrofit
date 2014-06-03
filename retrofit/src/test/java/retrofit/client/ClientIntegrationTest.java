//// Copyright 2014 Square, Inc.
//package retrofit.client;
//
//import com.squareup.okhttp.mockwebserver.MockResponse;
//import com.squareup.okhttp.mockwebserver.MockWebServer;
//import com.squareup.okhttp.mockwebserver.RecordedRequest;
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.List;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.junit.runners.Parameterized;
//import retrofit.RestAdapter;
//import retrofit.http.Body;
//import retrofit.http.GET;
//import retrofit.http.POST;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@RunWith(Parameterized.class)
//public class ClientIntegrationTest {
//  @Parameterized.Parameters
//  public static List<Object[]> clients() {
//    return Arrays.asList(new Object[][] {
//        { new OkClient() },
//        { new UrlConnectionClient() },
//        { new ApacheClient() }
//    });
//  }
//
//  private final Client client;
//
//  private MockWebServer server;
//  private Service service;
//
//  public ClientIntegrationTest(Client client) {
//    this.client = client;
//  }
//
//  @Before public void setUp() throws Exception {
//    server = new MockWebServer();
//    server.play();
//
//    RestAdapter restAdapter = new RestAdapter.Builder()
//        .setEndpoint("http://" + server.getHostName() + ":" + server.getPort())
//        .setClient(client)
//        .build();
//    service = restAdapter.create(Service.class);
//  }
//
//  @After public void tearDown() throws IOException {
//    server.shutdown();
//  }
//
//  private interface Service {
//    @GET("/get")
//    Response get();
//
//    @POST("/post")
//    Response post(@Body List<String> body);
//  }
//
//  @Test public void get() throws InterruptedException {
//    server.enqueue(new MockResponse().setBody("{}"));
//    service.get();
//
//    RecordedRequest request = server.takeRequest();
//    assertThat(request.getPath()).isEqualTo("/get");
//    assertThat(request.getBody()).isEmpty();
//  }
//
//  @Test public void post() throws InterruptedException {
//    server.enqueue(new MockResponse().setBody("{}"));
//    service.post(Arrays.asList("Hello", "World!"));
//
//    RecordedRequest request = server.takeRequest();
//    assertThat(request.getPath()).isEqualTo("/post");
//    assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
//    assertThat(request.getHeader("Content-Length")).isEqualTo("18");
//    assertThat(request.getUtf8Body()).isEqualTo("[\"Hello\",\"World!\"]");
//  }
//}
