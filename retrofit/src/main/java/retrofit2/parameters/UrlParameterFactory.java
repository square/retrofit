package retrofit2.parameters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;

import okhttp3.HttpUrl;
import retrofit2.ParameterHandler;
import retrofit2.RequestBuilder;
import retrofit2.Retrofit;
import retrofit2.http.Url;

public class UrlParameterFactory implements ParameterHandler.Factory {

  @Override
  public ParameterHandler<?> get(Annotation annotation, Type type, String relativeUrl,
      Annotation[] annotations, Annotation[] methodAnnotations, Retrofit retrofit) {

    if (annotation instanceof Url) {
      if (type == HttpUrl.class
          || type == String.class
          || type == URI.class
          || (type instanceof Class && "android.net.Uri".equals(((Class<?>) type).getName()))) {
        return new UrlParameter();
      } else {
        throw new IllegalArgumentException(
            "@Url must be okhttp3.HttpUrl, String, java.net.URI, or android.net.Uri type.");
      }
    }
    return null;
  }

  static final class UrlParameter extends ParameterHandler<Object> {
    @Override
    public void apply(RequestBuilder builder, Object value) {
      builder.setRelativeUrl(value);
    }
  }
}
