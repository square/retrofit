// Copyright 2015 Square, Inc.
package retrofit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.lang.reflect.Type;
import okio.Buffer;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public final class GsonConverterTest {
  private Converter converter;

  interface Example {
    String getName();
  }

  class Impl implements Example {
    private final String theName;

    Impl(String name) {
      theName = name;
    }

    @Override public String getName() {
      return theName;
    }
  }

  @Before public void setUp() {
    Gson gson = new GsonBuilder()
        .registerTypeAdapter(Example.class, new JsonSerializer<Example>() {
          @Override public JsonElement serialize(Example example, Type type,
              JsonSerializationContext json) {
            JsonObject object = new JsonObject();
            object.addProperty("name", example.getName());
            return object;
          }
        })
        .create();
    converter = new GsonConverter(gson);
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
