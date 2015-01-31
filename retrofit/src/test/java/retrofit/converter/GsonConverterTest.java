// Copyright 2015 Square, Inc.
package retrofit.converter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import org.junit.Before;
import org.junit.Test;
import retrofit.mime.TypedOutput;
import retrofit.mime.TypedString;

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
    TypedOutput output = converter.toBody(new Impl("value"), Impl.class);
    assertJson("{\"theName\":\"value\"}", output);
  }

  @Test public void serializationTypeUsed() throws IOException {
    TypedOutput output = converter.toBody(new Impl("value"), Example.class);
    assertJson("{\"name\":\"value\"}", output);
  }

  @Test public void deserialization() throws IOException {
    TypedString json = new TypedString("{\"theName\":\"value\"}");
    Impl impl = (Impl) converter.fromBody(json, Impl.class);
    assertEquals("value", impl.getName());
  }

  private void assertJson(String expected, TypedOutput output) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    output.writeTo(baos);
    String json = new String(baos.toByteArray(), "UTF-8");
    assertEquals(expected, json);
  }
}
