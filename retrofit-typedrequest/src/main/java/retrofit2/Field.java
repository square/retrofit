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

public class Field {
  private final String name;
  private final Object value;
  private final boolean encoded;

  public Field(String name, Object value, boolean encoded) {
    this.name = name;
    this.value = value;
    this.encoded = encoded;
  }

  public Field(String name, Object value) {
    this(name, value, false);
  }

  public String name() {
    return name;
  }

  public Object value() {
    return value;
  }

  public boolean encoded() {
    return encoded;
  }
}
