package retrofit;

import retrofit.client.Response;
import retrofit.converter.Converter;

import java.lang.reflect.Type;

public class RetrofitConversionError extends RetrofitResponseError {
  protected RetrofitConversionError(String url, Response response, Converter converter,
                                    Type successType, Throwable exception) {
    super(url, response, converter, successType, exception);
  }
}
