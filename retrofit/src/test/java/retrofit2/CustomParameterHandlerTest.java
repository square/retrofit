package retrofit2;

import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.helpers.ToStringConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

import static org.assertj.core.api.Assertions.assertThat;
import static retrofit2.Utils.checkNotNull;

public class CustomParameterHandlerTest {

  @Test
  public void pathParamNonPathParamAndTypedBytes() {
    class Example {
      @FormUrlEncoded //
      @POST("/foo") //
      Call<ResponseBody> method(@Field("foo%1$d") List<Object> fields, @Field("kit") String kit) {
        return null;
      }
    }

    List<Object> values = Arrays.<Object>asList("foo", "bar", null, 3);
    Request request = buildRequest(Example.class, new IndexedFieldParameterFactory(), values, "kat");
    assertBody(request.body(), "foo0=foo&foo1=bar&kit=kat");
  }

  static class IndexedFieldParameterFactory implements ParameterHandler.Factory {

    @Override
    public ParameterHandler<?> get(Annotation annotation, Type type, String relativeUrl,
        Annotation[] annotations, Annotation[] methodAnnotations, Retrofit retrofit) {

      if (annotation instanceof Field
          && Iterable.class.isAssignableFrom(Utils.getRawType(type))) {
        Field field = (Field) annotation;
        String name = field.value();
        boolean encoded = field.encoded();

        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
        Converter<?, String> converter = retrofit.stringConverter(iterableType, annotations);
        return new IndexedFieldParameter<>(name, converter, encoded);
      }
      return null;
    }

    static final class IndexedFieldParameter<T> implements ParameterHandler<Iterable<T>> {
      private final String name;
      private final Converter<T, String> valueConverter;
      private final boolean encoded;

      IndexedFieldParameter(String name, Converter<T, String> valueConverter, boolean encoded) {
        this.name = checkNotNull(name, "name == null");
        this.valueConverter = valueConverter;
        this.encoded = encoded;
      }

      @Override
      public void apply(RequestBuilder builder, Iterable<T> values) throws IOException {
        int i = 0;
        for (T value : values) {
          if (value == null) return; // Skip null values.
          builder.addFormField(formatName(i++), valueConverter.convert(value), encoded);
        }
      }

      private String formatName(int index) {
        return String.format(name, index);
      }
    }
  }

  private static void assertBody(RequestBody body, String expected) {
    assertThat(body).isNotNull();
    Buffer buffer = new Buffer();
    try {
      body.writeTo(buffer);
      assertThat(buffer.readUtf8()).isEqualTo(expected);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static Request buildRequest(Class<?> cls, ParameterHandler.Factory factory, Object... args) {
    final AtomicReference<Request> requestRef = new AtomicReference<>();
    okhttp3.Call.Factory callFactory = new okhttp3.Call.Factory() {
      @Override public okhttp3.Call newCall(Request request) {
        requestRef.set(request);
        throw new UnsupportedOperationException("Not implemented");
      }
    };

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addConverterFactory(new ToStringConverterFactory())
        .callFactory(callFactory)
        .addParameterHandlerFactory(factory)
        .build();

    Method method = TestingUtils.onlyMethod(cls);
    ServiceMethod<?> serviceMethod = retrofit.loadServiceMethod(method);
    OkHttpCall<?> okHttpCall = new OkHttpCall<>(serviceMethod, args);
    Call<?> call = (Call<?>) serviceMethod.callAdapter.adapt(okHttpCall);
    try {
      call.execute();
      throw new AssertionError();
    } catch (UnsupportedOperationException ignored) {
      return requestRef.get();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
