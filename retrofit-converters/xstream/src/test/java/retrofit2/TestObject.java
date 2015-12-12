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

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("test-object") final class TestObject {
  final int id;
  final String message;

  TestObject(int id, String message) {
    this.id = id;
    this.message = message;
  }

  @Override public int hashCode() {
    int hash = 17;
    hash = hash * 31 + id;
    hash = hash * 31 + (message == null ? 0 : message.hashCode());
    return hash;
  }

  @Override public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof TestObject)) return false;
    TestObject other = (TestObject) o;
    return id == other.id
            && (message == null ? other.message == null : message.equals(other.message));
  }
}
