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
package retrofit2.converter.simplexml;

import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;

@Default(value = DefaultType.FIELD)
final class MyObject {
  @Element private String message;
  @Element private int count;

  public MyObject() {}

  public MyObject(String message, int count) {
    this.message = message;
    this.count = count;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public int getCount() {
    return count;
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = result * 31 + count;
    result = result * 31 + (message == null ? 0 : message.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof MyObject)) return false;
    MyObject other = (MyObject) obj;
    return count == other.count
        && (message == null ? other.message == null : message.equals(other.message));
  }
}
