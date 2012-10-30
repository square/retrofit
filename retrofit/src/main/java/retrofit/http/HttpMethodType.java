package retrofit.http;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.apache.http.HttpMessage;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.protocol.HTTP;
import retrofit.io.TypedBytes;

/**
 * Describes the type of HTTP request to perform, GET, POST, etc.
 *
 * @author Patrick Forhan (patrick@squareup.com)
 */
enum HttpMethodType {

  GET {
    @Override HttpUriRequest createFrom(HttpRequestBuilder builder) throws URISyntaxException {
      URI uri = getParameterizedUri(builder);
      HttpGet request = new HttpGet(uri);
      addHeaders(request, builder);
      return request;
    }
  },

  POST {
    @Override HttpUriRequest createFrom(HttpRequestBuilder builder) throws URISyntaxException {
      URI uri = getUri(builder);
      HttpPost request = new HttpPost(uri);
      addParams(request, builder);
      addHeaders(request, builder);
      return request;
    }
  },

  PUT {
    @Override HttpUriRequest createFrom(HttpRequestBuilder builder) throws URISyntaxException {
      URI uri = getUri(builder);
      HttpPut request = new HttpPut(uri);
      addParams(request, builder);
      addHeaders(request, builder);
      return request;
    }
  },

  DELETE {
    @Override HttpUriRequest createFrom(HttpRequestBuilder builder) throws URISyntaxException {
      URI uri = getParameterizedUri(builder);
      HttpDelete request = new HttpDelete(uri);
      addHeaders(request, builder);
      return request;
    }
  };

  public HttpProfiler.Method profilerMethod() {
    return HttpProfiler.Method.valueOf(name());
  }

  /** Create a request object from HttpRequestBuilder. */
  abstract HttpUriRequest createFrom(HttpRequestBuilder builder) throws URISyntaxException;

  /** Gets a URI with no query parameters specified. */
  private static URI getUri(HttpRequestBuilder builder) throws URISyntaxException {
    return URIUtils.createURI(builder.getScheme(), builder.getHost(), -1, builder.getRelativePath(), null, null);
  }

  /** Gets a URI with parameters specified as query string parameters. */
  private static URI getParameterizedUri(HttpRequestBuilder builder) throws URISyntaxException {
    List<NameValuePair> queryParams = builder.getParamList(false);
    String queryString = URLEncodedUtils.format(queryParams, HTTP.UTF_8);
    return URIUtils.createURI(builder.getScheme(), builder.getHost(), -1, builder.getRelativePath(), queryString, null);
  }

  private static void addHeaders(HttpMessage message, HttpRequestBuilder builder) {
    String mimeType = builder.getMimeType();
    if (mimeType != null) {
      message.addHeader(HTTP.CONTENT_TYPE, mimeType);
    }
    Headers headers = builder.getHeaders();
    if (headers != null) {
      headers.setOn(message);
    }
  }

  /** Adds all but the last method argument as parameters of HTTP request object. */
  private static void addParams(HttpEntityEnclosingRequestBase request, HttpRequestBuilder builder) {
    Method method = builder.getMethod();
    Object[] args = builder.getArgs();
    Class<?>[] parameterTypes = method.getParameterTypes();

    Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    int count = parameterAnnotations.length;
    if (!builder.isSynchronous()) {
      count -= 1;
    }

    if (useMultipart(parameterTypes, parameterAnnotations)) {
      MultipartEntity form = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
      for (int i = 0; i < count; i++) {
        Object arg = args[i];
        if (arg == null) continue;
        Annotation[] annotations = parameterAnnotations[i];
        String name = HttpRequestBuilder.getName(annotations, method, i);
        Class<?> type = parameterTypes[i];

        if (TypedBytes.class.isAssignableFrom(type)) {
          TypedBytes typedBytes = (TypedBytes) arg;
          form.addPart(name, new TypedBytesBody(typedBytes, name));
        } else {
          try {
            form.addPart(name, new StringBody(String.valueOf(arg)));
          } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
          }
        }
      }
      request.setEntity(form);
    } else {
      try {
        if (builder.getSingleEntity() != null) {
          final TypedBytesEntity entity = new TypedBytesEntity(builder.getSingleEntity());
          request.setEntity(entity);
          request.addHeader(HTTP.CONTENT_TYPE, entity.getMimeType().mimeName());
        } else {
          List<NameValuePair> paramList = builder.getParamList(true);
          // TODO: Use specified encoding. (See CallbackResponseHandler et al)
          request.setEntity(new UrlEncodedFormEntity(paramList, HTTP.UTF_8));
        }
      } catch (UnsupportedEncodingException e) {
        throw new AssertionError(e);
      }
    }
  }

  /** Returns true if the parameters contain a file upload. */
  private static boolean useMultipart(Class<?>[] parameterTypes, Annotation[][] parameterAnnotations) {
    for (int i = 0; i < parameterTypes.length; i++) {
      Class<?> parameterType = parameterTypes[i];
      Annotation[] annotations = parameterAnnotations[i];
      if (TypedBytes.class.isAssignableFrom(parameterType) && !hasSingleEntityAnnotation(annotations)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasSingleEntityAnnotation(Annotation[] annotations) {
    for (Annotation annotation : annotations) {
      if (annotation.annotationType().equals(SingleEntity.class)) return true;
    }
    return false;
  }
}
