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

import com.squareup.moshi.Moshi;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.lang.reflect.Type;
import okio.Buffer;
import okio.BufferedSource;

/** A {@link Converter} which uses Moshi for serialization and deserialization of entities. */
public final class MoshiConverter implements Converter {
  private final Moshi moshi;
  private final MediaType mediaType;

  /** Create an instance using a default {@link Moshi} instance for conversion. */
  public MoshiConverter() {
    this(new Moshi.Builder().build());
  }

  /** Create an instance using the supplied {@link Moshi} object for conversion. */
  public MoshiConverter(Moshi moshi) {
    if (moshi == null) throw new NullPointerException("moshi == null");
    this.moshi = moshi;
    this.mediaType = MediaType.parse("application/json; charset=UTF-8");
  }

  @Override public Object fromBody(ResponseBody body, Type type) throws IOException {
    BufferedSource source = body.source();
    try {
      return moshi.adapter(type).fromJson(source);
    } finally {
      try {
        source.close();
      } catch (IOException ignored) {
      }
    }
  }

  @Override public RequestBody toBody(Object object, Type type) {
    Buffer buffer = new Buffer();
    try {
      moshi.adapter(type).toJson(buffer, object);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    return RequestBody.create(mediaType, buffer.snapshot());
  }
}
