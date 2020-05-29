/*
 * Copyright (C) 2020 Square, Inc.
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
package retrofit2.adapter.rxjava3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import org.junit.Test;
import retrofit2.Response;

public final class ResultTest {
  @Test
  public void response() {
    Response<String> response = Response.success("Hi");
    Result<String> result = Result.response(response);
    assertThat(result.isError()).isFalse();
    assertThat(result.error()).isNull();
    assertThat(result.response()).isSameAs(response);
  }

  @Test
  public void nullResponseThrows() {
    try {
      Result.response(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("response == null");
    }
  }

  @Test
  public void error() {
    Throwable error = new IOException();
    Result<Object> result = Result.error(error);
    assertThat(result.isError()).isTrue();
    assertThat(result.error()).isSameAs(error);
    assertThat(result.response()).isNull();
  }

  @Test
  public void nullErrorThrows() {
    try {
      Result.error(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("error == null");
    }
  }
}
