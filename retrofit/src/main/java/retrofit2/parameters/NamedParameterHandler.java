package retrofit2.parameters;

import java.io.IOException;

import retrofit2.ParameterHandler;
import retrofit2.RequestBuilder;

public class NamedParameterHandler<T> extends ParameterHandler<T> {
  private final String name;
  private final NamedValuesHandler<T> handler;

  public NamedParameterHandler(String name, NamedValuesHandler<T> handler) {
    this.name = name;
    this.handler = handler;
  }

  @Override
  public void apply(RequestBuilder builder, T value) throws IOException {
    handler.apply(builder, name, value);
  }
}
