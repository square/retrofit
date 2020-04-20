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
package retrofit2.converter.protobuf;

import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import java.io.IOException;
import javax.annotation.Nullable;
import okhttp3.ResponseBody;
import retrofit2.Converter;

final class ProtoResponseBodyConverter<T extends MessageLite>
    implements Converter<ResponseBody, T> {
  private final Parser<T> parser;
  private final @Nullable ExtensionRegistryLite registry;

  ProtoResponseBodyConverter(Parser<T> parser, @Nullable ExtensionRegistryLite registry) {
    this.parser = parser;
    this.registry = registry;
  }

  @Override
  public T convert(ResponseBody value) throws IOException {
    try {
      return registry == null
          ? parser.parseFrom(value.byteStream())
          : parser.parseFrom(value.byteStream(), registry);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e); // Despite extending IOException, this is data mismatch.
    } finally {
      value.close();
    }
  }
}
