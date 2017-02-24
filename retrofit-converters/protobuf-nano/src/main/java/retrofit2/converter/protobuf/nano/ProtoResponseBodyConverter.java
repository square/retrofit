/*
 * Copyright (C) 2016 Square, Inc.
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
package retrofit2.converter.protobuf.nano;

import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
import com.google.protobuf.nano.MessageNano;
import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Converter;

final class ProtoResponseBodyConverter<T extends MessageNano>
        implements Converter<ResponseBody, T> {
  private final Class<T> typeClass;

  ProtoResponseBodyConverter(Class<T> typeClass) {
    this.typeClass = typeClass;
  }

  @Override public T convert(ResponseBody value) throws IOException {
    try {
      return MessageNano.mergeFrom(getNanoProtoInstance(), value.bytes());
    } catch (InvalidProtocolBufferNanoException e) {
      throw new RuntimeException(e); // Despite extending IOException, this is data mismatch.
    } finally {
      value.close();
    }
  }

  private T getNanoProtoInstance() {
    try {
      return typeClass.newInstance();
    } catch (InstantiationException e) {
      throw new RuntimeException("Nanoproto instantiation failed", e);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }
}

