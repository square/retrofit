// Copyright 2013 Square, Inc.
package retrofit.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides POJO behavior for all of the APIs {@link retrofit.client.UrlConnectionClient}
 * interacts with.
 */
public class DummyHttpUrlConnection extends HttpURLConnection {
  private final Map<String, List<String>> responseHeaders =
      new LinkedHashMap<String, List<String>>();
  private final Map<String, List<String>> requestHeaders =
      new LinkedHashMap<String, List<String>>();
  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private int responseCode;
  private String responseMessage;
  private InputStream inputStream;
  private InputStream errorStream;

  protected DummyHttpUrlConnection(String url) throws MalformedURLException {
    super(new URL(url));
  }

  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  @Override public int getResponseCode() throws IOException {
    return responseCode;
  }

  public void setResponseMessage(String responseMessage) {
    this.responseMessage = responseMessage;
  }

  @Override public String getResponseMessage() throws IOException {
    return responseMessage;
  }

  @Override public ByteArrayOutputStream getOutputStream() throws IOException {
    return outputStream;
  }

  public void setInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  @Override public InputStream getInputStream() throws IOException {
    return inputStream;
  }

  public void setErrorStream(InputStream errorStream) {
    this.errorStream = errorStream;
  }

  @Override public InputStream getErrorStream() {
    return errorStream;
  }

  public void addResponseHeader(String name, String value) {
    List<String> values = responseHeaders.get(name);
    if (values == null) {
      values = new ArrayList<String>();
      responseHeaders.put(name, values);
    }
    values.add(value);
  }

  @Override public Map<String, List<String>> getHeaderFields() {
    return responseHeaders;
  }

  @Override public void addRequestProperty(String name, String value) {
    List<String> values = requestHeaders.get(name);
    if (values == null) {
      values = new ArrayList<String>();
      requestHeaders.put(name, values);
    }
    values.add(value);
  }

  @Override public Map<String, List<String>> getRequestProperties() {
    return requestHeaders;
  }

  @Override public String getRequestProperty(String name) {
    List<String> values = requestHeaders.get(name);
    if (values == null || values.isEmpty()) {
      return null;
    }
    return values.get(0);
  }

  @Override public void disconnect() {
    throw new AssertionError("Not implemented.");
  }

  @Override public boolean usingProxy() {
    return false;
  }

  @Override public void connect() throws IOException {
    throw new AssertionError("Not implemented.");
  }
}
