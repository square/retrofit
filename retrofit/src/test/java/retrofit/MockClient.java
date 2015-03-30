// Copyright 2015 Square, Inc.
package retrofit;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import static com.squareup.okhttp.Protocol.HTTP_1_1;

final class MockClient implements Interceptor {
  private Deque<Object> events = new ArrayDeque<Object>();
  private Deque<Request> requests = new ArrayDeque<Request>();

  @Override public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    requests.addLast(request);

    Object event = events.removeFirst();
    if (event instanceof IOException) {
      throw (IOException) event;
    }
    if (event instanceof RuntimeException) {
      throw (RuntimeException) event;
    }
    if (event instanceof Response.Builder) {
      Response.Builder response = (Response.Builder) event;
      return response.request(request).protocol(HTTP_1_1).build();
    }
    throw new IllegalStateException("Unknown event " + event.getClass());
  }

  public void enqueueResponse(Response.Builder response) {
    events.addLast(response);
  }

  public void enqueueUnexpectedException(RuntimeException exception) {
    events.addLast(exception);
  }

  public void enqueueIOException(IOException exception) {
    events.addLast(exception);
  }

  public Request takeRequest() {
    return requests.removeFirst();
  }
}
