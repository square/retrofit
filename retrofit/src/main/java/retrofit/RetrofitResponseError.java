package retrofit;

import retrofit.client.Response;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;

import java.lang.reflect.Type;

public abstract class RetrofitResponseError extends RetrofitError {

  protected Response response;
  protected Converter converter;
  protected Type successType;

  protected RetrofitResponseError(String url, Response response, Converter converter,
                                  Type successType, Throwable exception) {
    super("Request to " + url + " failed; response code is "
        + ((response == null) ? "unknown" : response.getStatus() + " " + response.getReason())
        + ".", url, exception);
    this.response = response;
    this.converter = converter;
    this.successType = successType;
  }

  protected RetrofitResponseError(String url, Response response, Converter converter,
                                  Type successType) {
    super("Request to " + url + " failed; response code is "
        + ((response == null) ? "unknown" : response.getStatus() + " " + response.getReason())
        + ".", url);
    this.response = response;
    this.converter = converter;
    this.successType = successType;
  }

  @Override
  @Deprecated
  public boolean isNetworkError() {
    return false;
  }

  @Override
  public Response getResponse() {
    return response;
  }

  @Override
  public Object getBody() {
    if (response == null) {
      return null;
    }

    TypedInput body = response.getBody();
    if (body == null) {
      return null;
    }

    if (converter == null) {
      throw new RuntimeException("Cannot convert body, supplied converter is null.");
    }

    try {
      return converter.fromBody(body, successType);
    } catch (ConversionException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Object getBodyAs(Type type) {
    if (response == null) {
      return null;
    }

    TypedInput body = response.getBody();
    if (body == null) {
      return null;
    }

    if (converter == null) {
      throw new RuntimeException("Cannot convert body, supplied converter is null.");
    }

    try {
      return converter.fromBody(body, type);
    } catch (ConversionException e) {
      throw new RuntimeException(e);
    }
  }
}
