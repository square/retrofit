package retrofit;

import retrofit.client.Response;
import retrofit.converter.Converter;

import java.lang.reflect.Type;

public class RetrofitHttpError extends RetrofitResponseError {
  protected RetrofitHttpError(String url, Response response, Converter converter,
                              Type successType) {
    super(url, response, converter, successType);
  }
}
