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
package retrofit;

import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.ResponseBody;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class ResponseTest {
  private final com.squareup.okhttp.Response successResponse =
      new com.squareup.okhttp.Response.Builder() //
          .code(200)
          .message("OK")
          .protocol(Protocol.HTTP_1_1)
          .request(new com.squareup.okhttp.Request.Builder().url("http://localhost").build())
          .build();
  private final com.squareup.okhttp.Response errorResponse =
      new com.squareup.okhttp.Response.Builder() //
          .code(400)
          .message("Broken!")
          .protocol(Protocol.HTTP_1_1)
          .request(new com.squareup.okhttp.Request.Builder().url("http://localhost").build())
          .build();

  @Test public void success() {
    Object body = new Object();
    Response<Object> response = Response.success(body);
    assertThat(response.raw()).isNotNull();
    assertThat(response.code()).isEqualTo(200);
    assertThat(response.message()).isEqualTo("OK");
    assertThat(response.headers().size()).isZero();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.body()).isSameAs(body);
    assertThat(response.errorBody()).isNull();
  }

  @Test public void successNullAllowed() {
    Response<Object> response = Response.success(null);
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.body()).isNull();
  }

  @Test public void successWithResponse() {
    Object body = new Object();
    Response<Object> response = Response.success(body, successResponse);
    assertThat(response.raw()).isSameAs(successResponse);
    assertThat(response.code()).isEqualTo(200);
    assertThat(response.message()).isEqualTo("OK");
    assertThat(response.headers().size()).isZero();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.body()).isSameAs(body);
    assertThat(response.errorBody()).isNull();
  }

  @Test public void successWithNullResponseThrows() {
    try {
      Response.success("", null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("rawResponse == null");
    }
  }

  @Test public void successWithErrorResponseThrows() {
    try {
      Response.success("", errorResponse);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("rawResponse must be successful response");
    }
  }

  @Test public void error() {
    ResponseBody errorBody = ResponseBody.create(null, "Broken!");
    Response<?> response = Response.error(400, errorBody);
    assertThat(response.raw()).isNotNull();
    assertThat(response.code()).isEqualTo(400);
    assertThat(response.message()).isNull();
    assertThat(response.headers().size()).isZero();
    assertThat(response.isSuccess()).isFalse();
    assertThat(response.body()).isNull();
    assertThat(response.errorBody()).isSameAs(errorBody);
  }

  @Test public void nullErrorThrows() {
    try {
      Response.error(400, null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("body == null");
    }
  }

  @Test public void errorWithSuccessCodeThrows() {
    ResponseBody errorBody = ResponseBody.create(null, "Broken!");
    try {
      Response.error(200, errorBody);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("code < 400: 200");
    }
  }

  @Test public void errorWithResponse() {
    ResponseBody errorBody = ResponseBody.create(null, "Broken!");
    Response<?> response = Response.error(errorBody, errorResponse);
    assertThat(response.raw()).isSameAs(errorResponse);
    assertThat(response.code()).isEqualTo(400);
    assertThat(response.message()).isEqualTo("Broken!");
    assertThat(response.headers().size()).isZero();
    assertThat(response.isSuccess()).isFalse();
    assertThat(response.body()).isNull();
    assertThat(response.errorBody()).isSameAs(errorBody);
  }

  @Test public void nullErrorWithResponseThrows() {
    try {
      Response.error(null, errorResponse);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("body == null");
    }
  }

  @Test public void errorWithNullResponseThrows() {
    ResponseBody errorBody = ResponseBody.create(null, "Broken!");
    try {
      Response.error(errorBody, null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("rawResponse == null");
    }
  }

  @Test public void errorWithSuccessResponseThrows() {
    ResponseBody errorBody = ResponseBody.create(null, "Broken!");
    try {
      Response.error(errorBody, successResponse);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("rawResponse should not be successful response");
    }
  }
}
