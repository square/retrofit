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
package retrofit2.converter.protobuf;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import java.lang.annotation.Annotation;
import okhttp3.ResponseBody;
import org.junit.Test;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.protobuf.PhoneProtos.Phone;

public final class FallbackParserFinderTest {
  @Test
  public void converterFactoryFallsBackToParserField() {
    Retrofit retrofit = new Retrofit.Builder().baseUrl("http://localhost/").build();
    ProtoConverterFactory factory = ProtoConverterFactory.create();
    Converter<ResponseBody, ?> converter =
        factory.responseBodyConverter(FakePhone.class, new Annotation[0], retrofit);
    assertThat(converter).isNotNull();
  }

  @SuppressWarnings("unused") // Used reflectively.
  public abstract static class FakePhone implements MessageLite {
    public static final Parser<Phone> PARSER = Phone.parser();
  }
}
