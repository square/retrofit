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

public class Query {

  private final String name;
  private final String value;
  private final boolean encoded;

  public Query(String name, String value) {
    this(name, value, false);
  }

  public Query(String name, String value, boolean encoded) {
    this.name = name;
    this.value = value;
    this.encoded = encoded;
  }

  public String name() {
    return name;
  }

  public String value() {
    return value;
  }

  public boolean encoded() {
    return encoded;
  }

  @Override public String toString() {
    return "Query{name: " + name + ", value: " + value + ", encoded: " + encoded + "}";
  }

  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Query query = (Query) o;

    if (encoded != query.encoded) {
      return false;
    }
    //noinspection SimplifiableIfStatement
    if (name != null ? !name.equals(query.name) : query.name != null) {
      return false;
    }
    return !(value != null ? !value.equals(query.value) : query.value != null);

  }

  @Override public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (value != null ? value.hashCode() : 0);
    result = 31 * result + (encoded ? 1 : 0);
    return result;
  }
}
