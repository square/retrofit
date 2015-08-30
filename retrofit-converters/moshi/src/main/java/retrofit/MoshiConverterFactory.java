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
package retrofit;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/** A {@linkplain Converter.Factory converter} which uses Moshi for JSON. */
public final class MoshiConverterFactory implements Converter.Factory {
  /** Create an instance using a default {@link Moshi} instance for conversion. */
  public static MoshiConverterFactory create() {
    return create(new Moshi.Builder().build());
  }

  /** Create an instance using {@code moshi} for conversion. */
  public static MoshiConverterFactory create(Moshi moshi) {
    return new MoshiConverterFactory(moshi);
  }

  private final Moshi moshi;

  private MoshiConverterFactory(Moshi moshi) {
    if (moshi == null) throw new NullPointerException("moshi == null");
    this.moshi = moshi;
  }

  /** Create a converter for {@code type}. */
  @Override public Converter<?> get(Type type, Annotation[] annotations) {
    JsonAdapter<Object> adapter = moshi.adapter(type);
    return new MoshiConverter<>(adapter);
  }
}
