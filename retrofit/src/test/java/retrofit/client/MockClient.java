package retrofit.client;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public final class MockClient implements Client {
  private final Deque<Object> events = new ArrayDeque<Object>();
  private final Deque<Request> requests = new ArrayDeque<Request>();

  public void enqueueResponse(Response response) {
    events.add(new ResponseEvent(response));
  }

  public void enqueueIOException(IOException exception) {
    events.add(new IOExceptionEvent(exception));
  }

  public void enqueueUnexpectedException(RuntimeException exception) {
    events.add(new UnexpectedExceptionEvent(exception));
  }

  public Request takeRequest() {
    return requests.pop();
  }

  @Override public void execute(Request request, AsyncCallback callback) {
    requests.add(request);

    Object event = events.pop();
    if (event instanceof ResponseEvent) {
      callback.onResponse(((ResponseEvent) event).response);
    } else if (event instanceof IOExceptionEvent) {
      callback.onFailure(((IOExceptionEvent) event).exception);
    } else if (event instanceof UnexpectedExceptionEvent) {
      throw ((UnexpectedExceptionEvent) event).exception;
    } else {
      throw new IllegalStateException("Unknown event " + event.getClass());
    }
  }

  private static final class ResponseEvent {
    final Response response;

    private ResponseEvent(Response response) {
      this.response = response;
    }
  }

  private static final class IOExceptionEvent {
    final IOException exception;

    private IOExceptionEvent(IOException exception) {
      this.exception = exception;
    }
  }

  private static final class UnexpectedExceptionEvent {
    final RuntimeException exception;

    private UnexpectedExceptionEvent(RuntimeException exception) {
      this.exception = exception;
    }
  }
}
