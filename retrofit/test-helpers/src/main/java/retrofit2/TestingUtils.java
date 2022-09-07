/*
 * Copyright (C) 2013 Square, Inc.
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

import java.lang.reflect.Method;
import java.util.Arrays;
import okhttp3.Request;
import retrofit2.helpers.ToStringConverterFactory;

final class TestingUtils {
  static <T> Request buildRequest(Class<T> cls, Retrofit.Builder builder, Object... args) {
    okhttp3.Call.Factory callFactory =
      request -> {
        throw new UnsupportedOperationException("Not implemented");
      };

    Retrofit retrofit = builder.callFactory(callFactory).build();

    Method method = onlyMethod(cls);
    try {
      return RequestFactory.parseAnnotations(retrofit, method).create(args);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  static <T> Request buildRequest(Class<T> cls, Object... args) {
    Retrofit.Builder retrofitBuilder =
      new Retrofit.Builder()
        .baseUrl("http://example.com/")
        .addConverterFactory(new ToStringConverterFactory());

    return buildRequest(cls, retrofitBuilder, args);
  }

  static Method onlyMethod(Class c) {
    Method[] declaredMethods = c.getDeclaredMethods();
    if (declaredMethods.length == 1) {
      return declaredMethods[0];
    }
    throw new IllegalArgumentException("More than one method declared.");
  }

  static String repeat(char c, int times) {
    char[] cs = new char[times];
    Arrays.fill(cs, c);
    return new String(cs);
  }
}
