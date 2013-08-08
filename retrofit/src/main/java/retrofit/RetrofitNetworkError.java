package retrofit;

import retrofit.client.Response;

import java.lang.reflect.Type;

public class RetrofitNetworkError extends RetrofitError {

  protected RetrofitNetworkError(String url, Throwable exception) {
    super("Request to " + url + " failed.", url, exception);
  }

  @Override
  @Deprecated
  public Response getResponse() {
    return null;
  }

  @Override
  @Deprecated
  public boolean isNetworkError() {
    return true;
  }

  @Override
  @Deprecated
  public Object getBody() {
    return null;
  }

  @Override
  @Deprecated
  public Object getBodyAs(Type type) {
    return null;
  }
}
