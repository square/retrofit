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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "contact")
final class Contact {
  @XmlElement(required = true)
  public final String name;

  @XmlElement(name = "phone_number")
  public final List<PhoneNumber> phone_numbers;

  @SuppressWarnings("unused") // Used by JAXB.
  private Contact() {
    this("", new ArrayList<PhoneNumber>());
  }

  public Contact(String name, List<PhoneNumber> phoneNumbers) {
    this.name = name;
    this.phone_numbers = phoneNumbers;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Contact
        && ((Contact) o).name.equals(name)
        && ((Contact) o).phone_numbers.equals(phone_numbers);
  }

  @Override
  public int hashCode() {
    return Arrays.asList(name, phone_numbers).hashCode();
  }
}
