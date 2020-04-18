/*
 * Copyright (C) 2016 Square, Inc.
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

import org.junit.Test;

public final class HttpExceptionTest {
  @Test
  public void response() {
    Response<String> response = Response.success("Hi");
    HttpException exception = new HttpException(response);
    assertThat(exception.code()).isEqualTo(200);
    assertThat(exception.message()).isEqualTo("OK");
    assertThat(exception.response()).isSameAs(response);
  }

  @Test
  public void nullResponseThrows() {
    try {
      new HttpException(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("response == null");
    }
  }
}
