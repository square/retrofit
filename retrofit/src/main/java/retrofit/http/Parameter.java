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
package retrofit.http;

import java.lang.reflect.Type;

/**
 * Represents a named parameter and its value.
 * <p>
 * This is used in one of three places in a request:
 * <ul>
 * <li>Named replacement in the relative URL path.
 * <li>As a URL query parameter.
 * <li>As a POST/PUT body.
 * </ul>
 */
public final class Parameter {
  private final String name;
  private final Type type;
  private final Object value;

  public Parameter(String name, Object value, Type valueType) {
    if (name == null) {
      throw new NullPointerException("name == null");
    }

    this.name = name;
    this.type = valueType;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public Object getValue() {
    return value;
  }

  /** The instance type of {@link #getValue()}. */
  public Type getValueType() {
    return type;
  }

  @Override public String toString() {
    return name + "=" + value;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Parameter parameter = (Parameter) o;

    if (!name.equals(parameter.name)) return false;
    if (value != null ? !value.equals(parameter.value) : parameter.value != null) return false;

    return true;
  }

  @Override public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }
}
