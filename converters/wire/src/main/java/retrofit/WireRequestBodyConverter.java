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

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.wire.Message;
import java.io.IOException;

final class WireRequestBodyConverter<T extends Message> implements Converter<T, RequestBody> {
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/x-protobuf");

  @Override public RequestBody convert(T value) throws IOException {
    byte[] bytes = value.toByteArray();
    return RequestBody.create(MEDIA_TYPE, bytes);
  }
}
