/*
 * Copyright (C) 2024 Square, Inc.
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import okhttp3.ResponseBody;
import org.junit.Test;
import retrofit2.helpers.ExampleWithoutParameterNames;
import retrofit2.http.GET;

public final class MethodParameterReflectionTest {
  private final Retrofit retrofit =
      new Retrofit.Builder().baseUrl("http://example.com/").validateEagerly(true).build();

  @Test
  public void paramIndexIsUsedWithoutParamReflection() {
    try {
      retrofit.create(ExampleWithoutParameterNames.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessageThat()
          .isEqualTo(
              "No Retrofit annotation found. (parameter #1)\n    for method ExampleWithoutParameterNames.method");
    }
  }

  /** This module is compiled with parameter names embedded in the class file. */
  interface ExampleWithParameterNames {
    @GET("/") //
    Call<ResponseBody> method(String theFirstParameter);
  }

  @Test
  public void paramNameIsUsedWithParamReflection() {
    try {
      retrofit.create(ExampleWithParameterNames.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessageThat()
          .isEqualTo(
              "No Retrofit annotation found. (parameter 'theFirstParameter')\n    for method ExampleWithParameterNames.method");
    }
  }
}
