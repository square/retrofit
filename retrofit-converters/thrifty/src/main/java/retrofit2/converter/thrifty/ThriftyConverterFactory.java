package retrofit2.converter.thrifty;

import com.microsoft.thrifty.Adapter;
import com.microsoft.thrifty.Struct;
import com.microsoft.thrifty.StructBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * A {@linkplain Converter.Factory converter} that uses Thrifty for Apache Thrift.
 * <p>
 * This converter only applies for types which extend from {@link Struct}.
 */
public final class ThriftyConverterFactory extends Converter.Factory {

  private ProtocolType protocolType;

  /**
   * Thrifty generates an adapter for each type as a static member called ADAPTER.
   * This method will get that adapter object for a given type.
   */
  @SuppressWarnings("unchecked")
  private static <T> Adapter<T, StructBuilder<T>> getAdapter(Class<T> type) {
    try {
      return (Adapter<T, StructBuilder<T>>) type.getField("ADAPTER").get(null);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new IllegalArgumentException("failed to access " + type.getName() + "#ADAPTER", e);
    }
  }

  public static ThriftyConverterFactory create(ProtocolType protocolType) {
    return new ThriftyConverterFactory(protocolType);
  }

  private ThriftyConverterFactory(ProtocolType protocolType) {
    this.protocolType = protocolType;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
      Retrofit retrofit) {
    if (!(type instanceof Class<?>)) {
      return null;
    }
    Class<?> c = (Class<?>) type;
    if (!Struct.class.isAssignableFrom(c)) {
      return null;
    }
    Adapter adapter = getAdapter(c);
    return new ThriftyResponseBodyConverter<>(protocolType, adapter);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Converter<?, RequestBody> requestBodyConverter(Type type,
      Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
    if (!(type instanceof Class<?>)) {
      return null;
    }
    Class<?> c = (Class<?>) type;
    if (!Struct.class.isAssignableFrom(c)) {
      return null;
    }
    Adapter adapter = getAdapter(c);
    return new ThriftyRequestBodyConverter<>(protocolType, adapter);
  }
}
