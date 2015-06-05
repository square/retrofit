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

public class Part {
  private final String name;
  private final Object value;
  private final String encoding;
  private final String filename;

  public Part(String name, Object value, String encoding, String filename) {
    this.name = name;
    this.value = value;
    this.encoding = encoding;
    this.filename = filename;
  }

  public Part(String name, Object value, String encoding) {
    this(name, value, encoding, null);
  }

  public Part(String name, Object value) {
    this(name, value, "binary");
  }

  public String name() {
    return name;
  }

  public Object value() {
    return value;
  }

  public String encoding() {
    return encoding;
  }

  public String filename() {
    return filename;
  }
}
