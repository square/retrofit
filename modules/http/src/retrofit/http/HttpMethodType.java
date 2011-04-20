package retrofit.http;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
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
import retrofit.io.TypedBytes;

/**
 * Describes the type of HTTP request to perform, GET, POST, etc.
 *
 * @author Patrick Forhan (patrick@squareup.com)
 */
public enum HttpMethodType {

  GET {
    @Override HttpUriRequest createFrom(HttpRequestBuilder builder)
        throws URISyntaxException {
      List<NameValuePair> queryParams = builder.getParamList(false);
      String queryString = URLEncodedUtils.format(queryParams, "UTF-8");
      URI uri = URIUtils.createURI(builder.getScheme(), builder.getHost(), -1,
          builder.getRelativePath(), queryString, null);
      HttpGet request = new HttpGet(uri);
      builder.getHeaders().setOn(request);
      return request;
    }
  },

  POST {
    @Override HttpUriRequest createFrom(HttpRequestBuilder builder)
        throws URISyntaxException {
      URI uri = URIUtils.createURI(builder.getScheme(), builder.getHost(), -1,
          builder.getRelativePath(), null, null);
      HttpPost request = new HttpPost(uri);
      addParams(request, builder);
      builder.getHeaders().setOn(request);
      return request;
    }
  },

  PUT {
    @Override HttpUriRequest createFrom(HttpRequestBuilder builder)
        throws URISyntaxException {
      URI uri = URIUtils.createURI(builder.getScheme(), builder.getHost(), -1,
          builder.getRelativePath(), null, null);
      HttpPut request = new HttpPut(uri);
      addParams(request, builder);
      builder.getHeaders().setOn(request);
      return request;
    }
  },

  DELETE {
    @Override HttpUriRequest createFrom(HttpRequestBuilder builder)
        throws URISyntaxException {
      List<NameValuePair> queryParams = builder.getParamList(false);
      String queryString = URLEncodedUtils.format(queryParams, "UTF-8");
      URI uri = URIUtils.createURI(builder.getScheme(), builder.getHost(), -1,
          builder.getRelativePath(), queryString, null);
      HttpDelete request = new HttpDelete(uri);
      builder.getHeaders().setOn(request);
      return request;
    }
  };

  public HttpProfiler.Method profilerMethod() {
    return HttpProfiler.Method.valueOf(name());
  }

  /**
   * Create a request object from HttpRequestBuilder.
   */
  abstract HttpUriRequest createFrom(HttpRequestBuilder builder)
      throws URISyntaxException;

  /**
   * Adds all but the last method argument as parameters of HTTP request
   * object.
   */
  private static void addParams(HttpEntityEnclosingRequestBase request,
      HttpRequestBuilder builder) {
    Method method = builder.getMethod();
    Object[] args = builder.getArgs();
    Class<?>[] parameterTypes = method.getParameterTypes();

    Annotation[][] parameterAnnotations =
        method.getParameterAnnotations();
    int count = parameterAnnotations.length - 1;

    if (useMultipart(parameterTypes)) {
      MultipartEntity form = new MultipartEntity(
          HttpMultipartMode.BROWSER_COMPATIBLE);
      for (int i = 0; i < count; i++) {
        Object arg = args[i];
        if (arg == null) continue;
        Annotation[] annotations = parameterAnnotations[i];
        String name = RestAdapter.getName(annotations, method, i);
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
        List<NameValuePair> paramList = builder.getParamList(true);
        request.setEntity(new UrlEncodedFormEntity(paramList));
      } catch (UnsupportedEncodingException e) {
        throw new AssertionError(e);
      }
    }
  }

  /** Returns true if the parameters contain a file upload. */
  private static boolean useMultipart(Class<?>[] parameterTypes) {
    for (Class<?> parameterType : parameterTypes) {
      if (TypedBytes.class.isAssignableFrom(parameterType)) return true;
    }
    return false;
  }

}