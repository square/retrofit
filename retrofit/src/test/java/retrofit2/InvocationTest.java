/*
 * Copyright (C) 2018 Square, Inc.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.junit.Test;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public final class InvocationTest {
  interface Example {
    @POST("/{p1}") //
    Call<ResponseBody> postMethod(
        @Path("p1") String p1, @Query("p2") String p2, @Body RequestBody body);
  }

  @Test
  public void invocationObjectOnCallAndRequestTag() {
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl("http://example.com/")
            .callFactory(new OkHttpClient())
            .build();

    Example example = retrofit.create(Example.class);
    RequestBody requestBody = RequestBody.create(MediaType.get("text/plain"), "three");
    Call<ResponseBody> call = example.postMethod("one", "two", requestBody);

    Invocation invocation = call.request().tag(Invocation.class);
    Method method = invocation.method();
    assertThat(method.getName()).isEqualTo("postMethod");
    assertThat(method.getDeclaringClass()).isEqualTo(Example.class);
    assertThat(invocation.arguments()).isEqualTo(Arrays.asList("one", "two", requestBody));
  }

  @Test
  public void nullMethod() {
    try {
      Invocation.of(null, Arrays.asList("one", "two"));
      fail();
    } catch (NullPointerException expected) {
      assertThat(expected).hasMessage("method == null");
    }
  }

  @Test
  public void nullArguments() {
    try {
      Invocation.of(Example.class.getDeclaredMethods()[0], null);
      fail();
    } catch (NullPointerException expected) {
      assertThat(expected).hasMessage("arguments == null");
    }
  }

  @Test
  public void argumentsAreImmutable() {
    List<String> mutableList = new ArrayList<>(Arrays.asList("one", "two"));
    Invocation invocation = Invocation.of(Example.class.getDeclaredMethods()[0], mutableList);
    mutableList.add("three");
    assertThat(invocation.arguments()).isEqualTo(Arrays.asList("one", "two"));
    try {
      invocation.arguments().clear();
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }
}
