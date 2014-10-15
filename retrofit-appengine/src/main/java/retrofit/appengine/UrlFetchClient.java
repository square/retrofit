package retrofit.appengine;

import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import retrofit.client.Client;
import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedOutput;

/** A {@link Client} for Google AppEngine's which uses its {@link URLFetchService}. */
public class UrlFetchClient implements Client {
  private static HTTPMethod getHttpMethod(String method) {
    if ("GET".equals(method)) {
      return HTTPMethod.GET;
    } else if ("POST".equals(method)) {
      return HTTPMethod.POST;
    } else if ("PATCH".equals(method)) {
      return HTTPMethod.PATCH;
    } else if ("PUT".equals(method)) {
      return HTTPMethod.PUT;
    } else if ("DELETE".equals(method)) {
      return HTTPMethod.DELETE;
    } else if ("HEAD".equals(method)) {
      return HTTPMethod.HEAD;
    } else {
      throw new IllegalStateException("Illegal HTTP method: " + method);
    }
  }

  private final URLFetchService urlFetchService;

  public UrlFetchClient() {
    this(URLFetchServiceFactory.getURLFetchService());
  }

  public UrlFetchClient(URLFetchService urlFetchService) {
    this.urlFetchService = urlFetchService;
  }

  @Override public Response execute(Request request) throws IOException {
    HTTPRequest fetchRequest = createRequest(request);
    HTTPResponse fetchResponse = execute(urlFetchService, fetchRequest);
    return parseResponse(fetchResponse, fetchRequest);
  }

  /** Execute the specified {@code request} using the provided {@code urlFetchService}. */
  protected HTTPResponse execute(URLFetchService urlFetchService, HTTPRequest request)
      throws IOException {
    return urlFetchService.fetch(request);
  }

  static HTTPRequest createRequest(Request request) throws IOException {
    HTTPMethod httpMethod = getHttpMethod(request.getMethod());
    URL url = new URL(request.getUrl());
    HTTPRequest fetchRequest = new HTTPRequest(url, httpMethod);

    for (Header header : request.getHeaders()) {
      fetchRequest.addHeader(new HTTPHeader(header.getName(), header.getValue()));
    }

    TypedOutput body = request.getBody();
    if (body != null) {
      String mimeType = body.mimeType();
      if (mimeType != null) {
        fetchRequest.addHeader(new HTTPHeader("Content-Type", mimeType));
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      body.writeTo(baos);
      fetchRequest.setPayload(baos.toByteArray());
    }

    return fetchRequest;
  }

  static Response parseResponse(HTTPResponse response, HTTPRequest creatingRequest) {
    // Response URL will be null if it is the same as the request URL.
    URL responseUrl = response.getFinalUrl();
    String urlString = (responseUrl != null ? responseUrl : creatingRequest.getURL()).toString();

    int status = response.getResponseCode();

    List<HTTPHeader> fetchHeaders = response.getHeaders();
    List<Header> headers = new ArrayList<Header>(fetchHeaders.size());
    String contentType = "application/octet-stream";
    for (HTTPHeader fetchHeader : fetchHeaders) {
      String name = fetchHeader.getName();
      String value = fetchHeader.getValue();
      if ("Content-Type".equalsIgnoreCase(name)) {
        contentType = value;
      }
      headers.add(new Header(name, value));
    }

    TypedByteArray body = null;
    byte[] fetchBody = response.getContent();
    if (fetchBody != null) {
      body = new TypedByteArray(contentType, fetchBody);
    }

    return new Response(urlString, status, "", headers, body);
  }
}
