/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import static retrofit2.CallAdapter.Factory.getParameterUpperBound;
import static retrofit2.CallAdapter.Factory.getRawType;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public final class CallAdapterTest {
  @Test
  public void parameterizedTypeInvalidIndex() {
    ParameterizedType listOfString = (ParameterizedType) new TypeToken<List<String>>() {}.getType();
    try {
      getParameterUpperBound(-1, listOfString);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessageThat()
          .isEqualTo("Index -1 not in range [0,1) for java.util.List<java.lang.String>");
    }
    try {
      getParameterUpperBound(1, listOfString);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessageThat()
          .isEqualTo("Index 1 not in range [0,1) for java.util.List<java.lang.String>");
    }
  }

  @Test
  public void parameterizedTypes() {
    ParameterizedType one = (ParameterizedType) new TypeToken<List<String>>() {}.getType();
    assertThat(getParameterUpperBound(0, one)).isSameInstanceAs(String.class);

    ParameterizedType two = (ParameterizedType) new TypeToken<Map<String, String>>() {}.getType();
    assertThat(getParameterUpperBound(0, two)).isSameInstanceAs(String.class);
    assertThat(getParameterUpperBound(1, two)).isSameInstanceAs(String.class);

    ParameterizedType wild =
        (ParameterizedType) new TypeToken<List<? extends CharSequence>>() {}.getType();
    assertThat(getParameterUpperBound(0, wild)).isSameInstanceAs(CharSequence.class);
  }

  @Test
  public void rawTypeThrowsOnNull() {
    try {
      getRawType(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessageThat().isEqualTo("type == null");
    }
  }

  @Test
  public void rawTypes() throws NoSuchMethodException {
    assertThat(getRawType(String.class)).isSameInstanceAs(String.class);

    Type listOfString = new TypeToken<List<String>>() {}.getType();
    assertThat(getRawType(listOfString)).isSameInstanceAs(List.class);

    Type stringArray = new TypeToken<String[]>() {}.getType();
    assertThat(getRawType(stringArray)).isSameInstanceAs(String[].class);

    Type wild =
        ((ParameterizedType) new TypeToken<List<? extends CharSequence>>() {}.getType())
            .getActualTypeArguments()[0];
    assertThat(getRawType(wild)).isSameInstanceAs(CharSequence.class);

    Type wildParam =
        ((ParameterizedType) new TypeToken<List<? extends List<String>>>() {}.getType())
            .getActualTypeArguments()[0];
    assertThat(getRawType(wildParam)).isSameInstanceAs(List.class);

    Type typeVar = A.class.getDeclaredMethod("method").getGenericReturnType();
    assertThat(getRawType(typeVar)).isSameInstanceAs(Object.class);
  }

  @SuppressWarnings("unused") // Used reflectively.
  static class A<T> {
    T method() {
      return null;
    }
  }
}
