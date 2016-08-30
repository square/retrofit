package retrofit2.parameters;

import java.io.IOException;

import retrofit2.RequestBuilder;

public interface NamedValuesHandler<T> {
  void apply(RequestBuilder builder, String name, T value) throws IOException;
}
