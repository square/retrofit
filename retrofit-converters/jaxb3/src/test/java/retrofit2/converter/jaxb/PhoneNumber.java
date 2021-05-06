/*
 * Copyright (C) 2018 Square, Inc.
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
package retrofit2.converter.jaxb;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.Arrays;
import javax.annotation.Nullable;

final class PhoneNumber {
  @XmlElement(required = true)
  public final String number;

  @XmlAttribute public final Type type;

  @SuppressWarnings("unused") // Used by JAXB.
  private PhoneNumber() {
    this("", Type.OTHER);
  }

  PhoneNumber(String number, @Nullable Type type) {
    this.number = number;
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof PhoneNumber
        && ((PhoneNumber) o).number.equals(number)
        && ((PhoneNumber) o).type.equals(type);
  }

  @Override
  public int hashCode() {
    return Arrays.asList(number, type).hashCode();
  }
}
