package retrofit.http;

import retrofit.http.client.Response;

/**
 * A wrapper that holds the {@link Response} and {@link Converter} response to be used by the
 * {@link CallbackRunnable} for success method calls on {@link Callback}.
 *
 * @author JJ Ford (jj.n.ford@gmail.com)
 */
final class ResponseWrapper {

  Response response;
  Object responseBody;

  public ResponseWrapper(Response response, Object responseBody) {
    this.response = response;
    this.responseBody = responseBody;
  }
}
