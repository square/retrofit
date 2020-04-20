/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.ResponseBody;
import org.junit.Test;

public final class ResponseTest {
  private final okhttp3.Response successResponse =
      new okhttp3.Response.Builder() //
          .code(200)
          .message("OK")
          .protocol(Protocol.HTTP_1_1)
          .request(new okhttp3.Request.Builder().url("http://localhost").build())
          .build();
  private final okhttp3.Response errorResponse =
      new okhttp3.Response.Builder() //
          .code(400)
          .message("Broken!")
          .protocol(Protocol.HTTP_1_1)
          .request(new okhttp3.Request.Builder().url("http://localhost").build())
          .build();

  @Test
  public void success() {
    Object body = new Object();
    Response<Object> response = Response.success(body);
    assertThat(response.raw()).isNotNull();
    assertThat(response.code()).isEqualTo(200);
    assertThat(response.message()).isEqualTo("OK");
    assertThat(response.headers().size()).isZero();
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isSameAs(body);
    assertThat(response.errorBody()).isNull();
  }

  @Test
  public void successNullAllowed() {
    Response<Object> response = Response.success(null);
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isNull();
  }

  @Test
  public void successWithHeaders() {
    Object body = new Object();
    Headers headers = Headers.of("foo", "bar");
    Response<Object> response = Response.success(body, headers);
    assertThat(response.raw()).isNotNull();
    assertThat(response.code()).isEqualTo(200);
    assertThat(response.message()).isEqualTo("OK");
    assertThat(response.headers().toMultimap()).isEqualTo(headers.toMultimap());
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isSameAs(body);
    assertThat(response.errorBody()).isNull();
  }

  @Test
  public void successWithNullHeadersThrows() {
    try {
      Response.success("", (okhttp3.Headers) null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("headers == null");
    }
  }

  @Test
  public void successWithStatusCode() {
    Object body = new Object();
    Response<Object> response = Response.success(204, body);
    assertThat(response.code()).isEqualTo(204);
    assertThat(response.message()).isEqualTo("Response.success()");
    assertThat(response.headers().size()).isZero();
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isSameAs(body);
    assertThat(response.errorBody()).isNull();
  }

  @Test
  public void successWithRawResponse() {
    Object body = new Object();
    Response<Object> response = Response.success(body, successResponse);
    assertThat(response.raw()).isSameAs(successResponse);
    assertThat(response.code()).isEqualTo(200);
    assertThat(response.message()).isEqualTo("OK");
    assertThat(response.headers().size()).isZero();
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isSameAs(body);
    assertThat(response.errorBody()).isNull();
  }

  @Test
  public void successWithNullRawResponseThrows() {
    try {
      Response.success("", (okhttp3.Response) null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("rawResponse == null");
    }
  }

  @Test
  public void successWithErrorRawResponseThrows() {
    try {
      Response.success("", errorResponse);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("rawResponse must be successful response");
    }
  }

  @Test
  public void error() {
    MediaType plainText = MediaType.get("text/plain; charset=utf-8");
    ResponseBody errorBody = ResponseBody.create(plainText, "Broken!");
    Response<?> response = Response.error(400, errorBody);
    assertThat(response.raw()).isNotNull();
    assertThat(response.raw().body().contentType()).isEqualTo(plainText);
    assertThat(response.raw().body().contentLength()).isEqualTo(7);
    try {
      response.raw().body().source();
      fail();
    } catch (IllegalStateException expected) {
    }
    assertThat(response.code()).isEqualTo(400);
    assertThat(response.message()).isEqualTo("Response.error()");
    assertThat(response.headers().size()).isZero();
    assertThat(response.isSuccessful()).isFalse();
    assertThat(response.body()).isNull();
    assertThat(response.errorBody()).isSameAs(errorBody);
  }

  @Test
  public void nullErrorThrows() {
    try {
      Response.error(400, null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("body == null");
    }
  }

  @Test
  public void errorWithSuccessCodeThrows() {
    ResponseBody errorBody = ResponseBody.create(null, "Broken!");
    try {
      Response.error(200, errorBody);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("code < 400: 200");
    }
  }

  @Test
  public void errorWithRawResponse() {
    ResponseBody errorBody = ResponseBody.create(null, "Broken!");
    Response<?> response = Response.error(errorBody, errorResponse);
    assertThat(response.raw()).isSameAs(errorResponse);
    assertThat(response.code()).isEqualTo(400);
    assertThat(response.message()).isEqualTo("Broken!");
    assertThat(response.headers().size()).isZero();
    assertThat(response.isSuccessful()).isFalse();
    assertThat(response.body()).isNull();
    assertThat(response.errorBody()).isSameAs(errorBody);
  }

  @Test
  public void nullErrorWithRawResponseThrows() {
    try {
      Response.error(null, errorResponse);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("body == null");
    }
  }

  @Test
  public void errorWithNullRawResponseThrows() {
    ResponseBody errorBody = ResponseBody.create(null, "Broken!");
    try {
      Response.error(errorBody, null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("rawResponse == null");
    }
  }

  @Test
  public void errorWithSuccessRawResponseThrows() {
    ResponseBody errorBody = ResponseBody.create(null, "Broken!");
    try {
      Response.error(errorBody, successResponse);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("rawResponse should not be successful response");
    }
  }
}
