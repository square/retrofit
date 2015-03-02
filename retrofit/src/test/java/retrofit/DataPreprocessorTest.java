package retrofit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.rule.MockWebServerRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DataPreprocessorTest {

  private interface ExampleInterface {
    @GET("/")
    ResponseTest something();

    @GET("/")
    void something(Callback<ResponseTest> callback);

    @POST("/")
    Object requestWrap(@Body RequestTest requestTest);

  }

  @Rule
  public final MockWebServerRule serverRule = new MockWebServerRule();

  private ExampleInterface exampleInterface;
  private TestPreprocessor testProcessor;

  @Before
  public void setUp() {
    OkHttpClient client = new OkHttpClient();

    testProcessor = new TestPreprocessor();

    exampleInterface = new RestAdapter.Builder() //
            .setClient(client)
            .setCallbackExecutor(new Utils.SynchronousExecutor())
            .setEndpoint(serverRule.getUrl("/").toString())
            .setDataPreprocessor(testProcessor)
            .build()
            .create(ExampleInterface.class);
  }

  @Test public void responseWrapTestDirect() throws Exception {
    Gson gson = new GsonBuilder().create();
    ResponseWrapperTest rwt = getMockResponseWrapper();
    serverRule.enqueue(new MockResponse().setBody(gson.toJson(rwt)));

    ResponseTest response = exampleInterface.something();
    assertThat(response.a).isEqualTo(42);
    assertThat(response.b).isEqualTo("Life");
    assertThat(testProcessor.otherData[0]).isEqualTo("response wrapper data 1");
  }

  @Test public void responseWrapTestAsync() throws Exception {
    Gson gson = new GsonBuilder().create();
    ResponseWrapperTest rwt = getMockResponseWrapper();
    serverRule.enqueue(new MockResponse().setBody(gson.toJson(rwt)));

    final AtomicReference<ResponseTest> responseRef = new AtomicReference<ResponseTest>();
    final CountDownLatch latch = new CountDownLatch(1);
    exampleInterface.something(new Callback<ResponseTest>() {
      @Override
      public void success(ResponseTest responseTest, Response response) {
        responseRef.set(responseTest);
        latch.countDown();
      }

      @Override
      public void failure(RetrofitError error) {
        throw new AssertionError();
      }
    });

    assertTrue(latch.await(1, TimeUnit.SECONDS));

    assertThat(responseRef.get().a).isEqualTo(42);
    assertThat(responseRef.get().b).isEqualTo("Life");
    assertThat(testProcessor.otherData[0]).isEqualTo("response wrapper data 1");

  }

  @Test public void requestWrapTest() throws Exception {
    Gson gson = new GsonBuilder().create();
    RequestTest requestTest = new RequestTest(7, "Wrapping");
    ResponseWrapperTest responseWrapperTest = getMockResponseWrapper();
    serverRule.enqueue(new MockResponse().setBody(gson.toJson(responseWrapperTest)));
    exampleInterface.requestWrap(requestTest);


    String body = new String(serverRule.takeRequest().getBody(), Charset.forName("UTF-8"));
    RequestWrapperTest serverRequestBody = gson.fromJson(body, RequestWrapperTest.class);
    RequestTest rtRequest = gson.fromJson(serverRequestBody.mainData, RequestTest.class);

    assertThat(serverRequestBody.otherData).isEqualTo("extra data 1");
    assertThat(rtRequest.intData).isEqualTo(7);
    assertThat(rtRequest.stringData).isEqualTo("Wrapping");
  }

  @Test public void dataProcessorNullTest() {
    OkHttpClient client = new OkHttpClient();
    try {
      new RestAdapter.Builder()
              .setClient(client)
              .setCallbackExecutor(new Utils.SynchronousExecutor())
              .setEndpoint(serverRule.getUrl("/").toString())
              .setDataPreprocessor(null)
              .build()
              .create(ExampleInterface.class);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("DataProcessor may not be null.");
    }
  }

  private ResponseWrapperTest getMockResponseWrapper() {
    Gson gson = new GsonBuilder().create();
    ResponseTest responseTest = new ResponseTest(42, "Life");
    ResponseWrapperTest rwt = new ResponseWrapperTest("response wrapper data 1", gson.toJson(responseTest));

    return rwt;
  }

  private static class TestPreprocessor implements DataPreprocessor {

    String[] otherData = new String[1];

    @Override
    public Object unWrapResponse(ResponseBody responseBody, Type type) {
      Gson gson = new GsonBuilder().create();
      Charset charset = Charset.forName("UTF-8");
      InputStream is = responseBody.byteStream();

      ResponseWrapperTest testClass = gson.fromJson(new InputStreamReader(is, charset), ResponseWrapperTest.class);
      otherData[0] = testClass.otherData;

      return gson.fromJson(testClass.mainData, type);
    }

    @Override
    public Object wrapRequest(Object object) {
      Gson gson = new GsonBuilder().create();
      RequestWrapperTest rwt = new RequestWrapperTest("extra data 1", gson.toJson(object, object.getClass()));
      return rwt;
    }
  }

  private static class ResponseWrapperTest {
    String otherData;
    String mainData;

    private ResponseWrapperTest(String otherData, String mainData) {
      this.otherData = otherData;
      this.mainData = mainData;
    }
  }

  private static class ResponseTest {
    int a;
    String b;

    private ResponseTest(int a, String b) {
      this.a = a;
      this.b = b;
    }
  }

  private static class RequestWrapperTest {
    String otherData;
    String mainData;

    private RequestWrapperTest(String otherData, String mainData) {
      this.otherData = otherData;
      this.mainData = mainData;
    }
  }

  private static class RequestTest {
    int intData;
    String stringData;

    private RequestTest(int intData, String stringData) {
      this.intData = intData;
      this.stringData = stringData;
    }
  }

}


