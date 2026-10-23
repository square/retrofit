/*
 * Copyright (C) 2024 Square, Inc.
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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * An immutable type (no no-arg constructor) that uses {@link XmlJavaTypeAdapter} to participate in
 * JAXB serialization. This is the pattern documented at
 * http://blog.bdoughan.com/2010/12/jaxb-and-immutable-objects.html
 */
@XmlJavaTypeAdapter(ImmutablePoint.Adapter.class)
final class ImmutablePoint {
  final int x;
  final int y;

  ImmutablePoint(int x, int y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ImmutablePoint
        && ((ImmutablePoint) o).x == x
        && ((ImmutablePoint) o).y == y;
  }

  @Override
  public int hashCode() {
    return 31 * x + y;
  }

  /** Mutable intermediate type used by JAXB. */
  @XmlRootElement(name = "point")
  static final class Mutable {
    @XmlElement public int x;
    @XmlElement public int y;
  }

  static final class Adapter extends XmlAdapter<Mutable, ImmutablePoint> {
    @Override
    public ImmutablePoint unmarshal(Mutable v) {
      return new ImmutablePoint(v.x, v.y);
    }

    @Override
    public Mutable marshal(ImmutablePoint v) {
      Mutable m = new Mutable();
      m.x = v.x;
      m.y = v.y;
      return m;
    }
  }
}
