package retrofit2.parameters;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Converter;
import retrofit2.ParameterHandler;
import retrofit2.RequestBuilder;
import retrofit2.Retrofit;
import retrofit2.http.Path;

import static retrofit2.Utils.checkNotNull;

public class PathParameterFactory implements ParameterHandler.Factory {

  // Upper and lower characters, digits, underscores, and hyphens, starting with a character.
  static final String PARAM = "[a-zA-Z][a-zA-Z0-9_-]*";
  static final Pattern PARAM_URL_REGEX = Pattern.compile("\\{(" + PARAM + ")\\}");
  static final Pattern PARAM_NAME_REGEX = Pattern.compile(PARAM);

  @Override
  public ParameterHandler<?> get(Annotation annotation, Type type, String relativeUrl,
      Annotation[] annotations, Annotation[] methodAnnotations, Retrofit retrofit) {

    if (annotation instanceof Path) {
      Path path = (Path) annotation;
      String name = path.value();
      validatePathName(name, relativeUrl);

      Converter<?, String> converter = retrofit.stringConverter(type, annotations);
      return new PathParameter<>(name, converter, path.encoded());
    }
    return null;
  }

  static final class PathParameter<T> extends ParameterHandler<T> {
    private final String name;
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    PathParameter(String name, Converter<T, String> valueConverter, boolean encoded) {
      this.name = checkNotNull(name, "name == null");
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override
    public void apply(RequestBuilder builder, T value) throws IOException {
      if (value == null) {
        throw new IllegalArgumentException(
            "Path parameter \"" + name + "\" value must not be null.");
      }
      builder.addPathParam(name, valueConverter.convert(value), encoded);
    }
  }

  private void validatePathName(String name, String relativeUrl) {
    if (!PARAM_NAME_REGEX.matcher(name).matches()) {
      throw new IllegalArgumentException(
          String.format("@Path parameter name must match %s. Found: %s",
          PARAM_URL_REGEX.pattern(), name));
    }

    // Verify URL replacement name is actually present in the URL path.
    if (!parsePathParameters(relativeUrl).contains(name)) {
      throw new IllegalArgumentException(
          String.format("URL \"%s\" does not contain \"{%s}\".", relativeUrl, name));
    }
  }

  /**
   * Gets the set of unique path parameters used in the given URI. If a parameter is used twice
   * in the URI, it will only show up once in the set.
   */
  static Set<String> parsePathParameters(String path) {
    Matcher m = PARAM_URL_REGEX.matcher(path);
    Set<String> patterns = new LinkedHashSet<>();
    while (m.find()) {
      patterns.add(m.group(1));
    }
    return patterns;
  }
}
