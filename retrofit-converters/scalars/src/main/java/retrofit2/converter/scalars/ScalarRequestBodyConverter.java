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
package retrofit2.converter.scalars;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Converter;

final class ScalarRequestBodyConverter<T> implements Converter<T, RequestBody> {
  static final ScalarRequestBodyConverter<Object> INSTANCE = new ScalarRequestBodyConverter<>();
  private static final MediaType MEDIA_TYPE = MediaType.get("text/plain; charset=UTF-8");

  private ScalarRequestBodyConverter() {}

  @Override
  public RequestBody convert(T value) throws IOException {
    return RequestBody.create(MEDIA_TYPE, String.valueOf(value));
  }
}
