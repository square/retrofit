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

import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.ToJson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import okio.Buffer;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public final class MoshiConverterTest {
  private Converter converter;

  interface Example {
    String getName();
  }

  static class Impl implements Example {
    private final String theName;

    Impl(String name) {
      theName = name;
    }

    @Override public String getName() {
      return theName;
    }
  }

  static class ExampleAdapter {
    @ToJson public void to(JsonWriter writer, Example example) throws IOException {
      writer.beginObject();
      writer.name("name").value(example.getName());
      writer.endObject();
    }

    @FromJson public Example from(JsonReader reader) throws IOException {
      throw new UnsupportedOperationException(); // Moshi requires this method to exist.
    }
  }

  @Before public void setUp() {
    Moshi gson = new Moshi.Builder()
        .add(new ExampleAdapter())
        .build();
    converter = new MoshiConverter(gson);
  }

  @Test public void serialization() throws IOException {
    RequestBody body = converter.toBody(new Impl("value"), Impl.class);
    assertBody(body).isEqualTo("{\"theName\":\"value\"}");
  }

  @Test public void serializationTypeUsed() throws IOException {
    RequestBody body = converter.toBody(new Impl("value"), Example.class);
    assertBody(body).isEqualTo("{\"name\":\"value\"}");
  }

  @Test public void deserialization() throws IOException {
    ResponseBody body =
        ResponseBody.create(MediaType.parse("text/plain"), "{\"theName\":\"value\"}");
    Impl impl = (Impl) converter.fromBody(body, Impl.class);
    assertEquals("value", impl.getName());
  }

  private static AbstractCharSequenceAssert<?, String> assertBody(RequestBody body) throws IOException {
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    return assertThat(buffer.readUtf8());
  }
}
