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

final class TestingUtils {
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
