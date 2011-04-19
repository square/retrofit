package retrofit.http;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Executor;
import junit.framework.TestCase;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicStatusLine;
import org.easymock.Capture;
import org.easymock.IAnswer;
import org.junit.Before;
import retrofit.core.Callback;
import retrofit.core.MainThread;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static retrofit.http.RestAdapter.service;

public class RestAdapterTest extends TestCase {
  private static final String ID = "123";
  private static final String ENTITY = "entity";
  private static final String ENTITY_PATH_PARAM = "entity/{id}";
  private static final String BASE_URL = "http://host/api/entity";
  private static final String PATH_URL_PREFIX = BASE_URL + "/";
  private static final String GET_DELETE_SIMPLE_URL = BASE_URL + "?";

  private Injector injector;
  private HttpClient mockHttpClient;
  private Executor mockExecutor;
  private MainThread mockMainThread;
  private Headers mockHeaders;
  @SuppressWarnings("rawtypes") private Callback mockCallback;
  private HttpResponse mockResponse;

  @Override @Before public void setUp() throws Exception {
    mockHttpClient = createMock(HttpClient.class);
    mockExecutor   = createMock(Executor.class);
    mockMainThread = createMock(MainThread.class);
    mockHeaders    = createMock(Headers.class);
    mockCallback   = createMock(Callback.class);
    mockResponse   = createMock(HttpResponse.class);

    injector = Guice.createInjector(
        new AbstractModule() {
          @Override protected void configure() {
            bind(Server.class).toInstance(new Server("http://host/api/",
                "http://host/web/", true));
            bind(HttpClient.class).toInstance(mockHttpClient);
            bind(Executor.class).toInstance(mockExecutor);
            bind(MainThread.class).toInstance(mockMainThread);
            bind(Headers.class).toInstance(mockHeaders);
            install(service(DeleteService.class));
            install(service(GetService.class));
            install(service(PostService.class));
            install(service(PutService.class));
          }
        });
  }

  @SuppressWarnings("unchecked")
  public void testServiceDeleteSimple() throws IOException {
    expectLifecycle(HttpDelete.class, GET_DELETE_SIMPLE_URL);
    replayAll();

    DeleteService service = injector.getInstance(DeleteService.class);
    service.delete(mockCallback);
    verifyAll();
  }

  @SuppressWarnings("unchecked")
  public void testServiceDeleteParam() throws IOException {
    expectLifecycle(HttpDelete.class, GET_DELETE_SIMPLE_URL + "id=" + ID);
    replayAll();

    DeleteService service = injector.getInstance(DeleteService.class);
    service.deleteWithParam(ID, mockCallback);
    verifyAll();
  }

  @SuppressWarnings("unchecked")
  public void testServiceDeletePathParam() throws IOException {
    expectLifecycle(HttpDelete.class, PATH_URL_PREFIX + ID + "?");
    replayAll();

    DeleteService service = injector.getInstance(DeleteService.class);
    service.deleteWithPathParam(ID, mockCallback);
    verifyAll();
  }

  @SuppressWarnings("unchecked")
  public void testServiceGetSimple() throws IOException {
    expectLifecycle(HttpGet.class, GET_DELETE_SIMPLE_URL);
    replayAll();

    GetService service = injector.getInstance(GetService.class);
    service.get(mockCallback);
    verifyAll();
  }

  @SuppressWarnings("unchecked")
  public void testServiceGetParam() throws IOException {
    expectLifecycle(HttpGet.class, GET_DELETE_SIMPLE_URL + "id=" + ID);
    replayAll();

    GetService service = injector.getInstance(GetService.class);
    service.getWithParam(ID, mockCallback);
    verifyAll();
  }

  @SuppressWarnings("unchecked")
  public void testServiceGetPathParam() throws IOException {
    expectLifecycle(HttpGet.class, PATH_URL_PREFIX + ID + "?");
    replayAll();

    GetService service = injector.getInstance(GetService.class);
    service.getWithPathParam(ID, mockCallback);
    verifyAll();
  }

  @SuppressWarnings("unchecked")
  public void testServicePostSimple() throws IOException {
    expectLifecycle(HttpPost.class, BASE_URL);
    replayAll();

    PostService service = injector.getInstance(PostService.class);
    service.post(mockCallback);
    verifyAll();
  }

  @SuppressWarnings("unchecked")
  public void testServicePostParam() throws IOException {
    expectLifecycle(HttpPost.class, BASE_URL);
    replayAll();

    PostService service = injector.getInstance(PostService.class);
    service.postWithParam(ID, mockCallback);
    verifyAll();
  }

  @SuppressWarnings("unchecked")
  public void testServicePostPathParam() throws IOException {
    expectLifecycle(HttpPost.class, PATH_URL_PREFIX + ID);
    replayAll();

    PostService service = injector.getInstance(PostService.class);
    service.postWithPathParam(ID, mockCallback);
    verifyAll();
  }

  @SuppressWarnings("unchecked")
  public void testServicePutSimple() throws IOException {
    expectLifecycle(HttpPut.class, BASE_URL);
    replayAll();

    PutService service = injector.getInstance(PutService.class);
    service.put(mockCallback);
    verifyAll();
  }

  @SuppressWarnings("unchecked")
  public void testServicePutParam() throws IOException {
    expectLifecycle(HttpPut.class, BASE_URL);
    replayAll();

    PutService service = injector.getInstance(PutService.class);
    service.putWithParam(ID, mockCallback);
    verifyAll();
  }

  @SuppressWarnings("unchecked")
  public void testServicePutPathParam() throws IOException {
    expectLifecycle(HttpPut.class, PATH_URL_PREFIX + ID);
    replayAll();

    PutService service = injector.getInstance(PutService.class);
    service.putWithPathParam(ID, mockCallback);
    verifyAll();
  }

  //
  // Utility Methods:
  //
  private void replayAll() {
    replay(mockExecutor, mockHeaders, mockHttpClient, mockMainThread,
        mockCallback, mockResponse);
  }

  private void verifyAll() {
    verify(mockExecutor, mockHeaders, mockHttpClient, mockMainThread,
        mockCallback, mockResponse);
  }

  private <T extends HttpUriRequest> void expectLifecycle(Class<T> requestClass,
      String requestUrl) throws UnsupportedEncodingException, IOException {
    expectExecution(mockExecutor);
    expectExecution(mockMainThread);
    expectSetOnWithRequest(requestClass, requestUrl);
    Response response = new Response("some text");
    expectResponseCalls(new Gson().toJson(response));
    expectHttpClientExecute();
    expectCall(response);
  }

  @SuppressWarnings("unchecked") private void expectCall(Response response) {
    mockCallback.call(response);
    expectLastCall().once();
  }

  private void expectHttpClientExecute() throws IOException {
    final Capture<GsonResponseHandler<?>> capture
        = new Capture<GsonResponseHandler<?>>();
    mockHttpClient.execute(isA(HttpUriRequest.class), capture(capture));
    expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override public Object answer() throws Throwable {
        GsonResponseHandler<?> responseHandler = capture.getValue();
        responseHandler.handleResponse(mockResponse);
        return null;
      }
    });
  }

  private void expectResponseCalls(String jsonToReturn)
      throws UnsupportedEncodingException {
    expect(mockResponse.getEntity()).andReturn(new StringEntity(jsonToReturn));
    expect(mockResponse.getStatusLine()).andReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, ""));
  }

  private <T extends HttpUriRequest> void expectSetOnWithRequest(
      final Class<T> expectedRequestClass, final String expectedUri) {
    final Capture<HttpMessage> capture = new Capture<HttpMessage>();
    mockHeaders.setOn(capture(capture));
    expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override public Object answer() throws Throwable {
        T request = expectedRequestClass.cast(capture.getValue());
        assertEquals("uri should match expectations", expectedUri, request.getURI().toString());
        return null;
      }
    });
  }

  private void expectExecution(Executor executor) {
    final Capture<Runnable> capture = new Capture<Runnable>();
    executor.execute(capture(capture));
    expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override public Object answer() throws Throwable {
        capture.getValue().run();
        return null;
      }
    });
  }

  private interface DeleteService {

    @DELETE(ENTITY) void delete(Callback<Response> callback);

    @DELETE(ENTITY) void deleteWithParam(@Named("id") String id,
        Callback<Response> callback);

    @DELETE(ENTITY_PATH_PARAM) void deleteWithPathParam(@Named("id") String id,
        Callback<Response> callback);
  }

  private interface GetService {
    @GET(ENTITY) void get(Callback<Response> callback);

    @GET(ENTITY) void getWithParam(@Named("id") String id,
        Callback<Response> callback);

    @GET(ENTITY_PATH_PARAM) void getWithPathParam(@Named("id") String id,
        Callback<Response> callback);
  }

  private interface PostService {
    @POST(ENTITY) void post(Callback<Response> callback);

    @POST(ENTITY) void postWithParam(@Named("id") String id,
        Callback<Response> callback);

    @POST(ENTITY_PATH_PARAM) void postWithPathParam(@Named("id") String id,
        Callback<Response> callback);
  }

  private interface PutService {
    @PUT(ENTITY) void put(Callback<Response> callback);

    @PUT(ENTITY) void putWithParam(@Named("id") String id,
        Callback<Response> callback);

    @PUT(ENTITY_PATH_PARAM) void putWithPathParam(@Named("id") String id,
        Callback<Response> callback);
  }

  private static class Response {
    final String text;
    public Response() {
      this("");
    }
    public Response(String text) {
      this.text = text;
    }
    @Override public int hashCode() {
      return 7;
    }
    @Override public boolean equals(Object obj) {
      return text.equals(((Response)obj).text);
    }

  }
}
