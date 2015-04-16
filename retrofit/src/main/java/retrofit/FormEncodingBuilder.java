/*
 * Copyright (C) 2014 Square, Inc.
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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import okio.Buffer;
import okio.BufferedSink;
import okio.ByteString;

final class FormEncodingBuilder {
  private final Buffer content = new Buffer();

  FormEncodingBuilder add(String name, boolean encodeName, String value, boolean encodeValue) {
    if (content.size() > 0) {
      content.writeByte('&');
    }
    try {
      if (encodeName) {
        name = URLEncoder.encode(name, "UTF-8");
      }
      if (encodeValue) {
        value = URLEncoder.encode(value, "UTF-8");
      }
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
    content.writeUtf8(name);
    content.writeByte('=');
    content.writeUtf8(value);
    return this;
  }

  RequestBody build() {
    if (content.size() == 0) {
      throw new IllegalStateException("Form encoded body must have at least one part.");
    }
    return new FormEncodingRequestBody(content.snapshot());
  }

  private static final class FormEncodingRequestBody extends RequestBody {
    private static final MediaType CONTENT_TYPE =
        MediaType.parse("application/x-www-form-urlencoded");

    private final ByteString snapshot;

    public FormEncodingRequestBody(ByteString snapshot) {
      this.snapshot = snapshot;
    }

    @Override public MediaType contentType() {
      return CONTENT_TYPE;
    }

    @Override public long contentLength() throws IOException {
      return snapshot.size();
    }

    @Override public void writeTo(BufferedSink sink) throws IOException {
      sink.write(snapshot);
    }
  }
}
