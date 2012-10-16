// Copyright 2012 Square, Inc.
package retrofit.http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
import org.junit.Ignore;
import org.junit.Test;
import retrofit.http.Callback.ServerError;

import javax.inject.Named;
import javax.inject.Provider;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.Executor;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.fest.assertions.Assertions.assertThat;

public class RestAdapterTest {
  private static final String ID = "123";
  private static final String ENTITY = "entity";
  private static final String ENTITY_PATH_PARAM = "entity/{id}";
  private static final String BASE_URL = "http://host/api/entity";
  private static final String PATH_URL_PREFIX = BASE_URL + "/";
  private static final String GET_DELETE_SIMPLE_URL = BASE_URL + "?";
  private static final Gson GSON = new Gson();

  private RestAdapter restAdapter;
  private HttpClient mockHttpClient;
  private Executor mockExecutor;
  private MainThread mockMainThread;
  private Headers mockHeaders;
  private ResponseCallback mockCallback;
  private HttpResponse mockResponse;

  @Before public void setUp() throws Exception {
    mockHttpClient = createMock(HttpClient.class);
    mockExecutor   = createMock(Executor.class);
    mockMainThread = createMock(MainThread.class);
    mockHeaders    = createMock(Headers.class);
    mockCallback   = createMock(ResponseCallback.class);
    mockResponse   = createMock(HttpResponse.class);

    Server server = new Server("http://host/api/");
    Provider<HttpClient> httpClientProvider = new Provider<HttpClient>() {
      @Override public HttpClient get() {
        return mockHttpClient;
      }
    };
    restAdapter = new RestAdapter(server, httpClientProvider, mockExecutor, mockMainThread, mockHeaders,
        new GsonConverter(GSON), HttpProfiler.NONE);
  }

  @Test public void testServiceDeleteSimple() throws IOException {
    expectLifecycle(HttpDelete.class, GET_DELETE_SIMPLE_URL);
    replayAll();

    DeleteService service = restAdapter.create(DeleteService.class);
    service.delete(mockCallback);
    verifyAll();
  }

  @Test public void testServiceDeleteParam() throws IOException {
    expectLifecycle(HttpDelete.class, GET_DELETE_SIMPLE_URL + "id=" + ID);
    replayAll();

    DeleteService service = restAdapter.create(DeleteService.class);
    service.deleteWithParam(ID, mockCallback);
    verifyAll();
  }

  @Test public void testServiceDeleteWithFixedParam() throws IOException {
    expectLifecycle(HttpDelete.class, GET_DELETE_SIMPLE_URL + "filter=merchant&id=" + ID);
    replayAll();

    DeleteService service = restAdapter.create(DeleteService.class);
    service.deleteWithFixedParam(ID, mockCallback);
    verifyAll();
  }

  @Test public void testServiceDeleteWithMultipleFixedParam() throws IOException {
    expectLifecycle(HttpDelete.class, GET_DELETE_SIMPLE_URL + "filter=merchant&name2=value2&"+ "id=" + ID);
    replayAll();

    DeleteService service = restAdapter.create(DeleteService.class);
    service.deleteWithMultipleFixedParams(ID, mockCallback);
    verifyAll();
  }

  @Test public void testServiceDeletePathParam() throws IOException {
    expectLifecycle(HttpDelete.class, PATH_URL_PREFIX + ID + "?");
    replayAll();

    DeleteService service = restAdapter.create(DeleteService.class);
    service.deleteWithPathParam(ID, mockCallback);
    verifyAll();
  }

  @Test public void testServiceGetSimple() throws IOException {
    expectLifecycle(HttpGet.class, GET_DELETE_SIMPLE_URL);
    replayAll();

    GetService service = restAdapter.create(GetService.class);
    service.get(mockCallback);
    verifyAll();
  }

  @Test public void testServiceGetParam() throws IOException {
    expectLifecycle(HttpGet.class, GET_DELETE_SIMPLE_URL + "id=" + ID);
    replayAll();

    GetService service = restAdapter.create(GetService.class);
    service.getWithParam(ID, mockCallback);
    verifyAll();
  }

  @Test public void testServiceGetWithFixedParam() throws IOException {
    expectLifecycle(HttpGet.class, GET_DELETE_SIMPLE_URL + "filter=merchant&id=" + ID);
    replayAll();

    GetService service = restAdapter.create(GetService.class);
    service.getWithFixedParam(ID, mockCallback);
    verifyAll();
  }

  @Test public void testServiceGetWithMultipleFixedParams() throws IOException {
    expectLifecycle(HttpGet.class, GET_DELETE_SIMPLE_URL + "filter=merchant&name2=value2&id=" + ID);
    replayAll();

    GetService service = restAdapter.create(GetService.class);
    service.getWithMultipleFixedParams(ID, mockCallback);
    verifyAll();
  }

  @Test public void testServiceGetPathParam() throws IOException {
    expectLifecycle(HttpGet.class, PATH_URL_PREFIX + ID + "?");
    replayAll();

    GetService service = restAdapter.create(GetService.class);
    service.getWithPathParam(ID, mockCallback);
    verifyAll();
  }

  @Test public void testServicePostSimple() throws IOException {
    expectLifecycle(HttpPost.class, BASE_URL);
    replayAll();

    PostService service = restAdapter.create(PostService.class);
    service.post(mockCallback);
    verifyAll();
  }

  @Test public void testServicePostSimpleClientError() throws IOException {
    expectLifecycleClientError(HttpPost.class, BASE_URL);
    replayAll();

    PostService service = restAdapter.create(PostService.class);
    service.post(mockCallback);
    verifyAll();
  }

  @Test public void testServicePostSimpleServerError() throws IOException {
    expectLifecycleServerError(HttpPost.class, BASE_URL);
    replayAll();

    PostService service = restAdapter.create(PostService.class);
    service.post(mockCallback);
    verifyAll();
  }

  @Test public void testServicePostParam() throws IOException {
    expectLifecycle(HttpPost.class, BASE_URL);
    replayAll();

    PostService service = restAdapter.create(PostService.class);
    service.postWithParam(ID, mockCallback);
    verifyAll();
  }

  @Test public void testServicePostPathParam() throws IOException {
    expectLifecycle(HttpPost.class, PATH_URL_PREFIX + ID);
    replayAll();

    PostService service = restAdapter.create(PostService.class);
    service.postWithPathParam(ID, mockCallback);
    verifyAll();
  }

  @Test public void testServicePutSimple() throws IOException {
    expectLifecycle(HttpPut.class, BASE_URL);
    replayAll();

    PutService service = restAdapter.create(PutService.class);
    service.put(mockCallback);
    verifyAll();
  }

  @Test public void testServicePutParam() throws IOException {
    expectLifecycle(HttpPut.class, BASE_URL);
    replayAll();

    PutService service = restAdapter.create(PutService.class);
    service.putWithParam(ID, mockCallback);
    verifyAll();
  }

  @Test public void testServicePutPathParam() throws IOException {
    expectLifecycle(HttpPut.class, PATH_URL_PREFIX + ID);
    replayAll();

    PutService service = restAdapter.create(PutService.class);
    service.putWithPathParam(ID, mockCallback);
    verifyAll();
  }

  @Test public void testConcreteCallbackTypes() {
    Type expected = Response.class;
    Method method = getTypeTestMethod("a");
    assertThat(RestAdapter.methodWantsSynchronousInvocation(method)).isFalse();
    assertThat(RestAdapter.getResponseObjectType(method, false)).as("a").isEqualTo(expected);
  }

  @Test public void testConcreteCallbackTypesWithParams() {
    Type expected = Response.class;
    Method method = getTypeTestMethod("b");
    assertThat(RestAdapter.methodWantsSynchronousInvocation(method)).isFalse();
    assertThat(RestAdapter.getResponseObjectType(method, false)).as("b").isEqualTo(expected);
  }

  @Test public void testGenericCallbackTypes() {
    Type expected = Response.class;
    Method method = getTypeTestMethod("c");
    assertThat(RestAdapter.methodWantsSynchronousInvocation(method)).isFalse();
    assertThat(RestAdapter.getResponseObjectType(method, false)).as("c").isEqualTo(expected);
  }

  @Test public void testGenericCallbackTypesWithParams() {
    Type expected = Response.class;
    Method method = getTypeTestMethod("d");
    assertThat(RestAdapter.methodWantsSynchronousInvocation(method)).isFalse();
    assertThat(RestAdapter.getResponseObjectType(method, false)).as("d").isEqualTo(expected);
  }

  @Test public void testWildcardGenericCallbackTypes() {
    Type expected = Response.class;
    Method method = getTypeTestMethod("e");
    assertThat(RestAdapter.methodWantsSynchronousInvocation(method)).isFalse();
    assertThat(RestAdapter.getResponseObjectType(method, false)).as("e").isEqualTo(expected);
  }

  @Test public void testGenericCallbackWithGenericType() {
    Type expected = new TypeToken<List<String>>() {}.getType();
    Method method = getTypeTestMethod("f");
    assertThat(RestAdapter.methodWantsSynchronousInvocation(method)).isFalse();
    assertThat(RestAdapter.getResponseObjectType(method, false)).as("f").isEqualTo(expected);
  }

  @Ignore // TODO support this case!
  @Test public void testExtendingGenericCallback() {
    Type expected = Response.class;
    Method method = getTypeTestMethod("g");
    assertThat(RestAdapter.methodWantsSynchronousInvocation(method)).isFalse();
    assertThat(RestAdapter.getResponseObjectType(method, false)).as("g").isEqualTo(expected);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingCallbackTypes() {
    Method method = getTypeTestMethod("h");
    assertThat(RestAdapter.methodWantsSynchronousInvocation(method)).isFalse();
    RestAdapter.getResponseObjectType(method, false);
  }

  @Test public void testSynchronousResponse() {
    Type expected = Response.class;
    Method method = getTypeTestMethod("x");
    assertThat(RestAdapter.methodWantsSynchronousInvocation(method)).isTrue();
    assertThat(RestAdapter.getResponseObjectType(method, true)).as("x").isEqualTo(expected);
  }

  @Test public void testSynchronousGenericResponse() {
    Type expected = new TypeToken<List<String>>() {}.getType();
    Method method = getTypeTestMethod("y");
    assertThat(RestAdapter.methodWantsSynchronousInvocation(method)).isTrue();
    assertThat(RestAdapter.getResponseObjectType(method, true)).as("y").isEqualTo(expected);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSynchronousWithAsyncCallback() {
    RestAdapter.methodWantsSynchronousInvocation(getTypeTestMethod("z"));
  }

  //
  // Utility Methods:
  //
  private void replayAll() {
    replay(mockExecutor, mockHeaders, mockHttpClient, mockMainThread, mockCallback, mockResponse);
  }

  private void verifyAll() {
    verify(mockExecutor, mockHeaders, mockHttpClient, mockMainThread, mockCallback, mockResponse);
  }

  private <T extends HttpUriRequest> void expectLifecycle(Class<T> requestClass, String requestUrl) throws IOException {
    Response response = expectCallAndResponse(requestClass, requestUrl);
    expectResponseCalls(GSON.toJson(response), HttpStatus.SC_OK);
    expectHttpClientExecute();
    expectCallbacks(response);
  }

  private <T extends HttpUriRequest> void expectLifecycleClientError(Class<T> requestClass,
      String requestUrl) throws IOException {
    Response response = expectCallAndResponse(requestClass, requestUrl);
    expectResponseCalls(GSON.toJson(response), HttpStatus.SC_CONFLICT);
    expectHttpClientExecute();
    expectClientErrorCallbacks(response, HttpStatus.SC_CONFLICT);
  }

  private <T extends HttpUriRequest> void expectLifecycleServerError(Class<T> requestClass,
      String requestUrl) throws IOException {
    expectCallAndResponse(requestClass, requestUrl);
    expectResponseCalls(GSON.toJson(new ServerError("danger, danger!")), HttpStatus.SC_NOT_IMPLEMENTED);
    expectHttpClientExecute();
    expectServerErrorCallbacks(HttpStatus.SC_NOT_IMPLEMENTED);
  }

  private <T extends HttpUriRequest> Response expectCallAndResponse(Class<T> requestClass, String requestUrl) {
    expectExecution(mockExecutor);
    expectExecution(mockMainThread); // For call()
    expectSetOnWithRequest(requestClass, requestUrl);
    return new Response("some text");
  }

  private void expectCallbacks(Response response) {
    mockCallback.call(response);
    expectLastCall().once();
  }

  private void expectClientErrorCallbacks(Response response, int statusCode) {
    mockCallback.clientError(response, statusCode);
    expectLastCall().once();
  }

  private void expectServerErrorCallbacks(int statusCode) {
    mockCallback.serverError(isA(ServerError.class), eq(statusCode));
    expectLastCall().once();
  }

  private void expectHttpClientExecute() throws IOException {
    final Capture<CallbackResponseHandler> capture = new Capture<CallbackResponseHandler>();
    mockHttpClient.execute(isA(HttpUriRequest.class), capture(capture));
    expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override public Object answer() throws Throwable {
        CallbackResponseHandler responseHandler = capture.getValue();
        responseHandler.handleResponse(mockResponse);
        return null;
      }
    });
  }

  private void expectResponseCalls(String jsonToReturn, int statusCode)
      throws UnsupportedEncodingException {
    expect(mockResponse.getEntity()).andReturn(new StringEntity(jsonToReturn));
    expect(mockResponse.getStatusLine())
        .andReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), statusCode, ""));
  }

  private <T extends HttpUriRequest> void expectSetOnWithRequest(final Class<T> expectedRequestClass,
        final String expectedUri) {
    final Capture<HttpMessage> capture = new Capture<HttpMessage>();
    final Capture<String> captureMime = new Capture<String>();
    mockHeaders.setOn(capture(capture), capture(captureMime));
    expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override public Object answer() throws Throwable {
        T request = expectedRequestClass.cast(capture.getValue());
        assertThat(request.getURI().toString()).isEqualTo(expectedUri);
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

    @DELETE(ENTITY) @QueryParam(name="filter", value="merchant")
    void deleteWithFixedParam(@Named("id") String id,
        Callback<Response> callback);

    @DELETE(ENTITY)
    @QueryParams({
      @QueryParam(name="filter", value="merchant"),
      @QueryParam(name="name2", value="value2")
    })
    void deleteWithMultipleFixedParams(@Named("id") String id,
        Callback<Response> callback);

    @DELETE(ENTITY_PATH_PARAM) void deleteWithPathParam(@Named("id") String id,
        Callback<Response> callback);
  }

  private interface GetService {
    @GET(ENTITY) void get(Callback<Response> callback);

    @GET(ENTITY) void getWithParam(@Named("id") String id,
        Callback<Response> callback);

    @GET(ENTITY) @QueryParam(name="filter", value="merchant")
    void getWithFixedParam(@Named("id") String id, Callback<Response> callback);

    @GET(ENTITY)
    @QueryParams({
      @QueryParam(name="filter", value="merchant"),
      @QueryParam(name="name2", value="value2")
    })
    void getWithMultipleFixedParams(@Named("id") String id,
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
    @SuppressWarnings("unused") public Response() {
      this("");
    }
    public Response(String text) {
      this.text = text;
    }
    @Override public int hashCode() {
      return 7;
    }
    @Override public boolean equals(Object obj) {
      return obj instanceof Response && text.equals(((Response)obj).text);
    }
  }

  @SuppressWarnings("UnusedDeclaration")
  private interface TypeTestService {
    // Asynchronous
    @GET(ENTITY) void a(ResponseCallback c);
    @GET(ENTITY) void b(@Named("id") String id, ResponseCallback c);
    @GET(ENTITY) void c(Callback<Response> c);
    @GET(ENTITY) void d(@Named("id") String id, Callback<Response> c);
    @GET(ENTITY) void e(Callback<? extends Response> c);
    @GET(ENTITY) void f(Callback<List<String>> c);
    @GET(ENTITY) void g(ExtendingCallback<Response> callback);
    @GET(ENTITY) void h(@Named("id") String id);
    // Synchronous
    @GET(ENTITY) Response x();
    @GET(ENTITY) List<String> y();
    @GET(ENTITY) Response z(Callback<Response> callback);
  }

  private static Method getTypeTestMethod(String name) {
    Method[] methods = TypeTestService.class.getDeclaredMethods();
    for (Method method : methods) {
      if (method.getName().equals(name)) {
        return method;
      }
    }
    throw new IllegalArgumentException("Unknown method '" + name + "' on " + TypeTestService.class.getSimpleName());
  }

  private interface ResponseCallback extends Callback<Response> {
  }

  private interface ExtendingCallback<T> extends Callback<T> {
  }
}
