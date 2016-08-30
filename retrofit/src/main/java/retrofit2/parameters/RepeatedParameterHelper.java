package retrofit2.parameters;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.ParameterHandler;
import retrofit2.RequestBuilder;
import retrofit2.Utils;

public class RepeatedParameterHelper {

  public static Type getItemType(Type type) {
    Class<?> rawParameterType = Utils.getRawType(type);
    if (Iterable.class.isAssignableFrom(rawParameterType)) {
      if (!(type instanceof ParameterizedType)) {
        throw new IllegalArgumentException(rawParameterType.getSimpleName()
            + " must include generic type (e.g., "
            + rawParameterType.getSimpleName()
            + "<String>)");
      }
      ParameterizedType parameterizedType = (ParameterizedType) type;
      return Utils.getParameterUpperBound(0, parameterizedType);
    } else if (rawParameterType.isArray()) {
      return Utils.boxIfPrimitive(rawParameterType.getComponentType());
    } else {
      return type;
    }
  }

  public static ParameterHandler<?> wrapIfRepeated(Type type, ParameterHandler<?> handler) {
    Class<?> rawParameterType = Utils.getRawType(type);
    if (Iterable.class.isAssignableFrom(rawParameterType)) {
      return new IterableParameterHandler<>(handler);
    } else if (rawParameterType.isArray()) {
      return new ArrayParameterHandler<>(handler);
    } else {
      return handler;
    }
  }

  public static class IterableParameterHandler<T> implements ParameterHandler<Iterable<T>> {
    private final ParameterHandler<T> handler;

    public IterableParameterHandler(ParameterHandler<T> handler) {
      this.handler = handler;
    }

    @Override
    public void apply(RequestBuilder builder, Iterable<T> values) throws IOException {
      if (values == null) return; // Skip null values.

      for (T value : values) {
        handler.apply(builder, value);
      }
    }
  }

  public static class ArrayParameterHandler<T> implements ParameterHandler<Object> {
    private final ParameterHandler<T> handler;

    public ArrayParameterHandler(ParameterHandler<T> handler) {
      this.handler = handler;
    }

    @Override
    public void apply(RequestBuilder builder, Object values) throws IOException {
      if (values == null) return; // Skip null values.

      for (int i = 0, size = Array.getLength(values); i < size; i++) {
        //noinspection unchecked
        handler.apply(builder, (T) Array.get(values, i));
      }
    }
  }
}
