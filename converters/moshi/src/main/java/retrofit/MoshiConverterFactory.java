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
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * A {@linkplain Converter.Factory converter} which uses Moshi for JSON.
 * <p>
 * Because Moshi is so flexible in the types it supports, this converter assumes that it can handle
 * all types. If you are mixing JSON serialization with something else (such as protocol buffers),
 * you must {@linkplain Retrofit.Builder#addConverterFactory(Converter.Factory) add this instance}
 * last to allow the other converters a chance to see their types.
 */
public final class MoshiConverterFactory extends Converter.Factory {
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

  @Override
  public Converter<ResponseBody, ?> fromResponseBody(Type type, Annotation[] annotations) {
    JsonAdapter<?> adapter = moshi.adapter(type);
    return new MoshiResponseBodyConverter<>(adapter);
  }

  @Override public Converter<?, RequestBody> toRequestBody(Type type, Annotation[] annotations) {
    JsonAdapter<?> adapter = moshi.adapter(type);
    return new MoshiRequestBodyConverter<>(adapter);
  }
}
