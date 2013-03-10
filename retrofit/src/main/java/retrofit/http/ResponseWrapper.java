package retrofit.http;

import retrofit.http.client.Response;

/**
 * A wrapper that holds the {@link Response} and {@link Converter} response to be used by the
 * {@link CallbackRunnable} for success method calls on {@link Callback}.
 *
 * @author JJ Ford (jj.n.ford@gmail.com)
 */
public class ResponseWrapper {

  private Response response;
  private Object responseObj;

  public ResponseWrapper(Response response, Object responseObj) {
    this.response = response;
    this.responseObj = responseObj;
  }

  public Response getResponse() {
    return this.response;
  }

  public Object getResponseObj() {
    return this.responseObj;
  }
}
