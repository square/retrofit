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

import static retrofit2.Utils.checkNotNull;

abstract class DefaultParameterHandler {

  abstract void apply(RequestBuilder builder);

  static final class DefaultField extends DefaultParameterHandler {
    private final String name;
    private final String value;
    private final boolean encoded;

    DefaultField(String name, String value, boolean encoded) {
      this.name = checkNotNull(name, "name == null");
      this.value = checkNotNull(value, "value == null");
      this.encoded = encoded;
    }

    @Override
    void apply(RequestBuilder builder) {
      builder.addFormField(name, value, encoded);
    }
  }
}
