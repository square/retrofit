package retrofit2.parameters;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import retrofit2.ParameterHandler;
import retrofit2.RequestBuilder;
import retrofit2.Utils;

public class MapParameterHandler<T> extends ParameterHandler<Map<String, T>> {

  public MapParameterHandler(NamedValuesHandler<T> handler, String handlerName) {
    this.handler = handler;
    this.handlerName = handlerName;
  }

  public static Type getValueType(Type type, Annotation annotation) {
    String annotationName = annotation.annotationType().getSimpleName();
    Class<?> rawParameterType = Utils.getRawType(type);
    if (!Map.class.isAssignableFrom(rawParameterType)) {
      throw  new IllegalArgumentException(String.format("@%1$s parameter type must be Map.",
          annotationName));
    }
    Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
    if (!(mapType instanceof ParameterizedType)) {
      throw new IllegalArgumentException(
          "Map must include generic types (e.g., Map<String, String>)");
    }
    ParameterizedType parameterizedType = (ParameterizedType) mapType;
    Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
    if (String.class != keyType) {
      throw  new IllegalArgumentException(String.format("@%1$s keys must be of type String: %2$s",
          annotationName, keyType));
    }
    return Utils.getParameterUpperBound(1, parameterizedType);
  }

  private final String handlerName;
  private final NamedValuesHandler<T> handler;

  @Override
  public void apply(RequestBuilder builder, Map<String, T> value) throws IOException {
    if (value == null) {
      throw new IllegalArgumentException(handlerName + " map was null.");
    }

    for (Map.Entry<String, T> entry : value.entrySet()) {
      String entryKey = entry.getKey();
      if (entryKey == null) {
        throw new IllegalArgumentException(handlerName + " map contained null key.");
      }
      T entryValue = entry.getValue();
      if (entryValue == null) {
        throw new IllegalArgumentException(
            handlerName + " map contained null value for key '" + entryKey + "'.");
      }
      handler.apply(builder, entryKey, entryValue);
    }
  }
}
