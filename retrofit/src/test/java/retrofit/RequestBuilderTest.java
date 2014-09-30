// Copyright 2013 Square, Inc.
package retrofit;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.converter.Converter;
import retrofit.converter.GsonConverter;
import retrofit.http.Part;
import retrofit.http.PartMap;
import retrofit.mime.MimeHelper;
import retrofit.mime.MultipartTypedOutput;
import retrofit.mime.TypedOutput;
import retrofit.mime.TypedString;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static retrofit.RestMethodInfo.ParamUsage;
import static retrofit.RestMethodInfo.ParamUsage.BODY;
import static retrofit.RestMethodInfo.ParamUsage.ENCODED_PATH;
import static retrofit.RestMethodInfo.ParamUsage.ENCODED_QUERY;
import static retrofit.RestMethodInfo.ParamUsage.ENCODED_QUERY_MAP;
import static retrofit.RestMethodInfo.ParamUsage.FIELD;
import static retrofit.RestMethodInfo.ParamUsage.FIELD_MAP;
import static retrofit.RestMethodInfo.ParamUsage.HEADER;
import static retrofit.RestMethodInfo.ParamUsage.PART;
import static retrofit.RestMethodInfo.ParamUsage.PART_MAP;
import static retrofit.RestMethodInfo.ParamUsage.PATH;
import static retrofit.RestMethodInfo.ParamUsage.QUERY;
import static retrofit.RestMethodInfo.ParamUsage.QUERY_MAP;
import static retrofit.RestMethodInfo.RequestType;
import static retrofit.mime.MultipartTypedOutput.DEFAULT_TRANSFER_ENCODING;

public class RequestBuilderTest {
  @Test public void normalGet() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithPathParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/") //
        .addPathParam("ping", "pong") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithEncodedPathParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/") //
        .addEncodedPathParam("ping", "po%20ng") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/po%20ng/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithInterceptorPathParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/") //
        .addInterceptorPathParam("ping", "pong") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithPathParamAndInterceptorPathParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/{kit}/") //
        .addPathParam("ping", "pong") //
        .addInterceptorPathParam("kit", "kat")
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong/kat/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithInterceptorQueryParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addInterceptorQueryParam("butter", "finger") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/?butter=finger");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithPathParamAndInterceptorQueryParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/") //
        .addPathParam("ping", "pong") //
        .addInterceptorQueryParam("butter", "finger")
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong/?butter=finger");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithInterceptorPathParamAndInterceptorQueryParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/") //
        .addInterceptorPathParam("ping", "pong") //
        .addInterceptorQueryParam("butter", "finger") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong/?butter=finger");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithPathParamAndInterceptorPathParamAndInterceptorQueryParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/{kit}/") //
        .addPathParam("ping", "pong") //
        .addInterceptorPathParam("kit", "kat")
        .addInterceptorQueryParam("butter", "finger")
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong/kat/?butter=finger");
    assertThat(request.getBody()).isNull();
  }

  @Test public void pathParamRequired() throws Exception {
    try {
      new Helper() //
          .setMethod("GET") //
          .setUrl("http://example.com") //
          .setPath("/foo/bar/{ping}/") //
          .addPathParam("ping", null) //
          .build();
      fail("Null path parameters not allowed.");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Path parameter \"ping\" value must not be null.");
    }
  }

  @Test public void getWithQueryParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addQueryParam("ping", "pong") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/?ping=pong");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithEncodedQueryParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addEncodedQueryParam("ping", "p+o+n+g") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/?ping=p+o+n+g");
    assertThat(request.getBody()).isNull();
  }

  @Test public void queryParamOptional() throws Exception {
    Request request1 = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addQueryParam("ping", null) //
        .build();
    assertThat(request1.getUrl()).isEqualTo("http://example.com/foo/bar/");

    Request request2 = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addQueryParam("foo", "bar") //
        .addQueryParam("ping", null) //
        .addQueryParam("kit", "kat") //
        .build();
    assertThat(request2.getUrl()).isEqualTo("http://example.com/foo/bar/?foo=bar&kit=kat");
  }

  @Test public void getWithQueryUrlAndParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .setQuery("hi=mom") //
        .addQueryParam("ping", "pong") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/?hi=mom&ping=pong");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithQuery() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .setQuery("hi=mom") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/?hi=mom");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithPathAndQueryParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/") //
        .addPathParam("ping", "pong") //
        .addQueryParam("kit", "kat") //
        .addQueryParam("riff", "raff") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong/?kit=kat&riff=raff");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithPathAndQueryQuestionMarkParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/") //
        .addPathParam("ping", "pong?") //
        .addQueryParam("kit", "kat?") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong%3F/?kit=kat%3F");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithPathAndQueryAmpersandParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/") //
        .addPathParam("ping", "pong&") //
        .addQueryParam("kit", "kat&") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong%26/?kit=kat%26");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithPathAndQueryHashParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/") //
        .addPathParam("ping", "pong#") //
        .addQueryParam("kit", "kat#") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong%23/?kit=kat%23");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithQueryParamList() throws Exception {
    List<Object> values = Arrays.<Object>asList(1, 2, null, "three");

    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addQueryParam("key", values) //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/?key=1&key=2&key=three");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithQueryParamArray() throws Exception {
    Object[] values = { 1, 2, null, "three" };

    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addQueryParam("key", values) //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/?key=1&key=2&key=three");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithQueryParamPrimitiveArray() throws Exception {
    int[] values = { 1, 2, 3 };

    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addQueryParam("key", values) //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/?key=1&key=2&key=3");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithQueryParamMap() throws Exception {
    Map<String, Object> params = new LinkedHashMap<String, Object>();
    params.put("kit", "kat");
    params.put("foo", null);
    params.put("ping", "pong");

    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addQueryMapParams("options", params) //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/?kit=kat&ping=pong");
    assertThat(request.getBody()).isNull();
  }

  @Test public void getWithEncodedQueryParamMap() throws Exception {
    Map<String, Object> params = new LinkedHashMap<String, Object>();
    params.put("kit", "k%20t");
    params.put("foo", null);
    params.put("ping", "p%20g");

    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addEncodedQueryMapParams("options", params) //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/?kit=k%20t&ping=p%20g");
    assertThat(request.getBody()).isNull();
  }

  @Test public void normalPost() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .build();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void normalPostWithPathParam() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/") //
        .addPathParam("ping", "pong") //
        .build();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void body() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .setBody(Arrays.asList("quick", "brown", "fox")) //
        .build();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertTypedBytes(request.getBody(), "[\"quick\",\"brown\",\"fox\"]");
  }

  @Test public void bodyRequired() throws Exception {
    try {
      new Helper() //
          .setMethod("POST") //
          .setHasBody() //
          .setUrl("http://example.com") //
          .setPath("/foo/bar/") //
          .setBody(null) //
          .build();
      fail("Null body not allowed.");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Body parameter value must not be null.");
    }
  }

  @Test public void bodyWithPathParams() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/{ping}/{kit}/") //
        .addPathParam("ping", "pong") //
        .setBody(Arrays.asList("quick", "brown", "fox")) //
        .addPathParam("kit", "kat") //
        .build();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/pong/kat/");
    assertTypedBytes(request.getBody(), "[\"quick\",\"brown\",\"fox\"]");
  }

  @Test public void simpleMultipart() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .setMultipart() //
        .addPart("ping", "pong") //
        .addPart("kit", new TypedString("kat")) //
        .build();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");

    MultipartTypedOutput body = (MultipartTypedOutput) request.getBody();
    List<byte[]> bodyParts = MimeHelper.getParts(body);
    assertThat(bodyParts).hasSize(2);

    Iterator<byte[]> iterator = bodyParts.iterator();

    String one = new String(iterator.next(), "UTF-8");
    assertThat(one).contains("name=\"ping\"\r\n").endsWith("\r\npong");

    String two = new String(iterator.next(), "UTF-8");
    assertThat(two).contains("name=\"kit\"").endsWith("\r\nkat");
  }

  @Test public void multipartWithEncoding() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .setMultipart() //
        .addPart("ping", "8-bit", "pong") //
        .addPart("kit", "7-bit", new TypedString("kat")) //
        .build();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");

    MultipartTypedOutput body = (MultipartTypedOutput) request.getBody();
    List<byte[]> bodyParts = MimeHelper.getParts(body);
    assertThat(bodyParts).hasSize(2);

    Iterator<byte[]> iterator = bodyParts.iterator();

    String one = new String(iterator.next(), "UTF-8");
    assertThat(one).contains("name=\"ping\"\r\n")
        .contains("Content-Transfer-Encoding: 8-bit")
        .endsWith("\r\npong");

    String two = new String(iterator.next(), "UTF-8");
    assertThat(two).contains("name=\"kit\"")
        .contains("Content-Transfer-Encoding: 7-bit")
        .endsWith("\r\nkat");
  }

  @Test public void multipartPartMap() throws Exception {
    Map<String, Object> params = new LinkedHashMap<String, Object>();
    params.put("ping", "pong");
    params.put("kit", new TypedString("kat"));

    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .setMultipart() //
        .addPartMap("params", params) //
        .build();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");

    MultipartTypedOutput body = (MultipartTypedOutput) request.getBody();
    List<byte[]> bodyParts = MimeHelper.getParts(body);
    assertThat(bodyParts).hasSize(2);

    Iterator<byte[]> iterator = bodyParts.iterator();

    String one = new String(iterator.next(), "UTF-8");
    assertThat(one).contains("name=\"ping\"\r\n").endsWith("\r\npong");

    String two = new String(iterator.next(), "UTF-8");
    assertThat(two).contains("name=\"kit\"").endsWith("\r\nkat");
  }

  @Test public void multipartPartMapWithEncoding() throws Exception {
    Map<String, Object> params = new LinkedHashMap<String, Object>();
    params.put("ping", "pong");
    params.put("kit", new TypedString("kat"));

    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .setMultipart() //
        .addPartMap("params", "8-bit", params) //
        .build();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");

    MultipartTypedOutput body = (MultipartTypedOutput) request.getBody();
    List<byte[]> bodyParts = MimeHelper.getParts(body);
    assertThat(bodyParts).hasSize(2);

    Iterator<byte[]> iterator = bodyParts.iterator();

    String one = new String(iterator.next(), "UTF-8");
    assertThat(one).contains("name=\"ping\"\r\n")
        .contains("Content-Transfer-Encoding: 8-bit")
        .endsWith("\r\npong");

    String two = new String(iterator.next(), "UTF-8");
    assertThat(two).contains("name=\"kit\"")
        .contains("Content-Transfer-Encoding: 8-bit")
        .endsWith("\r\nkat");
  }

  @Test public void multipartNullRemovesPart() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .setMultipart() //
        .addPart("ping", "pong") //
        .addPart("fizz", null) //
        .build();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeaders()).isEmpty();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");

    MultipartTypedOutput body = (MultipartTypedOutput) request.getBody();
    List<byte[]> bodyParts = MimeHelper.getParts(body);
    assertThat(bodyParts).hasSize(1);

    Iterator<byte[]> iterator = bodyParts.iterator();

    String one = new String(iterator.next(), "UTF-8");
    assertThat(one).contains("name=\"ping\"").endsWith("\r\npong");
  }

  @Test public void multipartPartOptional() throws Exception {
    try {
      new Helper() //
          .setMethod("POST") //
          .setHasBody() //
          .setUrl("http://example.com") //
          .setPath("/foo/bar/") //
          .setMultipart() //
          .addPart("ping", null) //
          .build();
      fail("Empty multipart request is not allowed.");
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Multipart requests must contain at least one part.");
    }
  }

  @Test public void simpleFormEncoded() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo") //
        .setFormEncoded() //
        .addField("foo", "bar") //
        .addField("ping", "pong") //
        .build();
    assertTypedBytes(request.getBody(), "foo=bar&ping=pong");
  }

  @Test public void formEncodedFieldOptional() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo") //
        .setFormEncoded() //
        .addField("foo", "bar") //
        .addField("ping", null) //
        .addField("kit", "kat") //
        .build();
    assertTypedBytes(request.getBody(), "foo=bar&kit=kat");
  }

  @Test public void formEncodedFieldList() throws Exception {
    List<Object> values = Arrays.<Object>asList("foo", "bar", null, 3);

    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo") //
        .setFormEncoded() //
        .addField("foo", values) //
        .addField("kit", "kat") //
        .build();
    assertTypedBytes(request.getBody(), "foo=foo&foo=bar&foo=3&kit=kat");
  }

  @Test public void formEncodedFieldArray() throws Exception {
    Object[] values = { 1, 2, null, "three" };

    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo") //
        .setFormEncoded() //
        .addField("foo", values) //
        .addField("kit", "kat") //
        .build();
    assertTypedBytes(request.getBody(), "foo=1&foo=2&foo=three&kit=kat");
  }

  @Test public void formEncodedFieldPrimitiveArray() throws Exception {
    int[] values = { 1, 2, 3 };

    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo") //
        .setFormEncoded() //
        .addField("foo", values) //
        .addField("kit", "kat") //
        .build();
    assertTypedBytes(request.getBody(), "foo=1&foo=2&foo=3&kit=kat");
  }

  @Test public void formEncodedFieldMap() throws Exception {
    Map<String, Object> params = new LinkedHashMap<String, Object>();
    params.put("kit", "kat");
    params.put("foo", null);
    params.put("ping", "pong");

    Request request = new Helper() //
        .setMethod("POST") //
        .setHasBody() //
        .setUrl("http://example.com") //
        .setPath("/foo") //
        .setFormEncoded() //
        .addFieldMap("options", params) //
        .build();
    assertTypedBytes(request.getBody(), "kit=kat&ping=pong");
  }

  @Test public void simpleHeaders() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addHeaders("ping: pong", "kit: kat") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()) //
        .containsExactly(new Header("ping", "pong"), new Header("kit", "kat"));
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void simpleInterceptorHeaders() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addInterceptorHeader("ping", "pong") //
        .addInterceptorHeader("kit", "kat") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()) //
        .containsExactly(new Header("ping", "pong"), new Header("kit", "kat"));
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void headersAndInterceptorHeaders() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addHeaders("ping: pong") //
        .addInterceptorHeader("kit", "kat") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()) //
        .containsExactly(new Header("ping", "pong"), new Header("kit", "kat"));
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void allThreeHeaderTypes() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addHeaders("ping: pong") //
        .addInterceptorHeader("kit", "kat") //
        .addHeaderParam("fizz", "buzz") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).containsExactly(new Header("ping", "pong"),
        new Header("kit", "kat"), new Header("fizz", "buzz"));
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void methodHeader() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addHeaders("ping: pong") //
        .addHeaderParam("kit", "kat") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()) //
        .containsExactly(new Header("ping", "pong"), new Header("kit", "kat"));
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void headerParamToString() throws Exception {
    Object toStringHeaderParam = new BigInteger("1234");
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addHeaderParam("kit", toStringHeaderParam) //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()) //
        .containsExactly(new Header("kit", "1234"));
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void headerParam() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com") //
        .setPath("/foo/bar/") //
        .addHeaders("ping: pong") //
        .addHeaderParam("kit", "kat") //
        .build();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()) //
        .containsExactly(new Header("ping", "pong"), new Header("kit", "kat"));
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.getBody()).isNull();
  }

  @Test public void noDuplicateSlashes() throws Exception {
    Request request = new Helper() //
        .setMethod("GET") //
        .setUrl("http://example.com/") //
        .setPath("/foo/bar/") //
        .build();
    assertThat(request.getUrl()).isEqualTo("http://example.com/foo/bar/");
  }

  @Test public void contentTypeAnnotationHeaderOverrides() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setUrl("http://example.com") //
        .setPath("/") //
        .addHeaders("Content-Type: text/not-plain") //
        .setBody(new TypedString("Plain")) //
        .build();
    assertThat(request.getBody().mimeType()).isEqualTo("text/not-plain");
  }

  @Test public void contentTypeAnnotationHeaderAddsHeaderWithNoBody() throws Exception {
    Request request = new Helper() //
        .setMethod("DELETE") //
        .setUrl("http://example.com") //
        .setPath("/") //
        .addHeaders("Content-Type: text/not-plain") //
        .build();
    assertThat(request.getHeaders()).contains(new Header("Content-Type", "text/not-plain"));
  }

  @Test public void contentTypeInterceptorHeaderAddsHeaderWithNoBody() throws Exception {
    Request request = new Helper() //
        .setMethod("DELETE") //
        .setUrl("http://example.com") //
        .setPath("/") //
        .addInterceptorHeader("Content-Type", "text/not-plain") //
        .build();
    assertThat(request.getHeaders()).contains(new Header("Content-Type", "text/not-plain"));
  }

  @Test public void contentTypeParameterHeaderOverrides() throws Exception {
    Request request = new Helper() //
        .setMethod("POST") //
        .setUrl("http://example.com") //
        .setPath("/") //
        .addHeaderParam("Content-Type", "text/not-plain") //
        .setBody(new TypedString("Plain")) //
        .build();
    assertThat(request.getBody().mimeType()).isEqualTo("text/not-plain");
  }

  private static void assertTypedBytes(TypedOutput bytes, String expected) throws IOException {
    assertThat(bytes).isNotNull();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    bytes.writeTo(baos);
    assertThat(new String(baos.toByteArray(), "UTF-8")).isEqualTo(expected);
  }

  private static class Helper {
    private static final Converter GSON = new GsonConverter(new Gson());

    private RequestType requestType = RequestType.SIMPLE;
    private String method;
    private boolean hasBody = false;
    private String path;
    private String query;
    private final List<String> paramNames = new ArrayList<String>();
    private final List<ParamUsage> paramUsages = new ArrayList<ParamUsage>();
    private final List<Annotation> paramAnnotations = new ArrayList<Annotation>();
    private final List<Object> args = new ArrayList<Object>();
    private final List<String> headers = new ArrayList<String>();
    private final List<Header> interceptorHeaders = new ArrayList<Header>();
    private final Map<String, String> interceptorPathParams = new LinkedHashMap<String, String>();
    private final Map<String, String> interceptorQueryParams = new LinkedHashMap<String, String>();
    private String url;

    Helper setMethod(String method) {
      this.method = method;
      return this;
    }

    Helper setHasBody() {
      hasBody = true;
      return this;
    }

    Helper setUrl(String url) {
      this.url = url;
      return this;
    }

    Helper setPath(String path) {
      this.path = path;
      return this;
    }

    Helper setQuery(String query) {
      this.query = query;
      return this;
    }

    Helper addPathParam(String name, Object value) {
      paramNames.add(name);
      paramUsages.add(PATH);
      paramAnnotations.add(null); // Not used.
      args.add(value);
      return this;
    }

    Helper addEncodedPathParam(String name, String value) {
      paramNames.add(name);
      paramUsages.add(ENCODED_PATH);
      paramAnnotations.add(null); // Not used.
      args.add(value);
      return this;
    }

    Helper addQueryParam(String name, Object value) {
      paramNames.add(name);
      paramUsages.add(QUERY);
      paramAnnotations.add(null); // Not used.
      args.add(value);
      return this;
    }

    Helper addEncodedQueryParam(String name, String value) {
      paramNames.add(name);
      paramUsages.add(ENCODED_QUERY);
      paramAnnotations.add(null); // Not used.
      args.add(value);
      return this;
    }

    Helper addQueryMapParams(String name, Map<String, Object> values) {
      paramNames.add(name);
      paramUsages.add(QUERY_MAP);
      paramAnnotations.add(null); // Not used.
      args.add(values);
      return this;
    }

    Helper addEncodedQueryMapParams(String name, Map<String, Object> values) {
      paramNames.add(name);
      paramUsages.add(ENCODED_QUERY_MAP);
      paramAnnotations.add(null); // Not used.
      args.add(values);
      return this;
    }

    Helper addField(String name, Object value) {
      paramNames.add(name);
      paramUsages.add(FIELD);
      paramAnnotations.add(null); // Not used.
      args.add(value);
      return this;
    }

    Helper addFieldMap(String name, Map<String, Object> values) {
      paramNames.add(name);
      paramUsages.add(FIELD_MAP);
      paramAnnotations.add(null); // Not used.
      args.add(values);
      return this;
    }

    Helper addPart(final String name, Object value) {
      return addPart(name, DEFAULT_TRANSFER_ENCODING, value);
    }

    Helper addPart(final String name, final String transferEncoding, Object value) {
      paramNames.add(name);
      paramUsages.add(PART);
      paramAnnotations.add(new Part() {
        @Override public Class<? extends Annotation> annotationType() {
          return Part.class;
        }

        @Override public String value() {
          return name;
        }

        @Override public String encoding() {
          return transferEncoding;
        }
      });
      args.add(value);
      return this;
    }

    Helper addPartMap(String name, Map<String, Object> values) {
      return addPartMap(name, DEFAULT_TRANSFER_ENCODING, values);
    }

    Helper addPartMap(String name, final String transferEncoding, Map<String, Object> values) {
      paramNames.add(name);
      paramUsages.add(PART_MAP);
      paramAnnotations.add(new PartMap() {
        @Override public Class<? extends Annotation> annotationType() {
          return PartMap.class;
        }

        @Override public String encoding() {
          return transferEncoding;
        }
      });
      args.add(values);
      return this;
    }

    Helper setBody(Object value) {
      paramNames.add(null);
      paramUsages.add(BODY);
      paramAnnotations.add(null); // Not used.
      args.add(value);
      return this;
    }

    Helper addHeaderParam(String name, Object value) {
      paramNames.add(name);
      paramUsages.add(HEADER);
      paramAnnotations.add(null); // Not used.
      args.add(value);
      return this;
    }

    Helper addHeaders(String... headers) {
      Collections.addAll(this.headers, headers);
      return this;
    }

    Helper addInterceptorHeader(String name, String value) {
      interceptorHeaders.add(new Header(name, value));
      return this;
    }

    Helper addInterceptorPathParam(String name, String value) {
      interceptorPathParams.put(name, value);
      return this;
    }

    Helper addInterceptorQueryParam(String name, String value) {
      interceptorQueryParams.put(name, value);
      return this;
    }

    Helper setMultipart() {
      requestType = RequestType.MULTIPART;
      return this;
    }

    Helper setFormEncoded() {
      requestType = RequestType.FORM_URL_ENCODED;
      return this;
    }

    Request build() throws Exception {
      if (method == null) {
        throw new IllegalStateException("Method must be set.");
      }
      if (path == null) {
        throw new IllegalStateException("Path must be set.");
      }

      Method method = getClass().getDeclaredMethod("dummySync");

      RestMethodInfo methodInfo = new RestMethodInfo(method);
      methodInfo.requestMethod = this.method;
      methodInfo.requestHasBody = hasBody;
      methodInfo.requestType = requestType;
      methodInfo.requestUrl = path;
      methodInfo.requestUrlParamNames = RestMethodInfo.parsePathParameters(path);
      methodInfo.requestQuery = query;
      methodInfo.requestParamNames = paramNames.toArray(new String[paramNames.size()]);
      methodInfo.requestParamUsage = paramUsages.toArray(new ParamUsage[paramUsages.size()]);
      methodInfo.requestParamAnnotation =
          paramAnnotations.toArray(new Annotation[paramAnnotations.size()]);
      methodInfo.headers = headers.isEmpty() ? null
          : methodInfo.parseHeaders(headers.toArray(new String[headers.size()]));
      methodInfo.loaded = true;

      RequestBuilder requestBuilder = new RequestBuilder(url, methodInfo, GSON);

      // Simulate request interceptor invocation.
      for (Header header : interceptorHeaders) {
        requestBuilder.addHeader(header.getName(), header.getValue());
      }
      for (Map.Entry<String, String> entry : interceptorPathParams.entrySet()) {
        requestBuilder.addPathParam(entry.getKey(), entry.getValue());
      }
      for (Map.Entry<String, String> entry : interceptorQueryParams.entrySet()) {
        requestBuilder.addQueryParam(entry.getKey(), entry.getValue());
      }

      requestBuilder.setArguments(args.toArray(new Object[args.size()]));

      return requestBuilder.build();
    }

    @SuppressWarnings("UnusedDeclaration") // Accessed via reflection.
    private Object dummySync() {
      return null;
    }
  }
}
