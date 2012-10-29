// Copyright 2012 Square, Inc.
package retrofit.http;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import retrofit.io.MimeType;
import retrofit.io.TypedBytes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;

/**
 * A {@link Converter} which uses GSON for serialization and deserialization of entities.
 *
 * @author Jake Wharton (jw@squareup.com)
 */
public class GsonConverter implements Converter {
  private final Gson gson;

  public GsonConverter(Gson gson) {
    this.gson = gson;
  }

  @Override public Object to(byte[] body, Type type) throws ConversionException {
    try {
      // TODO use actual encoding
      InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(body), "UTF-8");
      return gson.fromJson(isr, type);
    } catch (IOException e) {
      throw new ConversionException(e);
    } catch (JsonParseException e) {
      throw new ConversionException(e);
    }
  }

  @Override public TypedBytes from(Object object) {
    return new JsonTypedBytes(gson, object);
  }

  private static class JsonTypedBytes implements TypedBytes {
    private final String json;

    JsonTypedBytes(Gson gson, Object object) {
      json = gson.toJson(object);
    }

    @Override public MimeType mimeType() {
      return MimeType.JSON;
    }

    @Override public int length() {
      return json.length();
    }

    @Override public void writeTo(OutputStream out) throws IOException {
      out.write(json.getBytes("UTF-8")); // TODO use actual encoding
    }
  }
}
