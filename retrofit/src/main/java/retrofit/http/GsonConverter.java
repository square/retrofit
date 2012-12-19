// Copyright 2012 Square, Inc.
package retrofit.http;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import retrofit.io.MimeType;
import retrofit.io.TypedBytes;

import static retrofit.http.RestAdapter.UTF_8;

/**
 * A {@link Converter} which uses GSON for serialization and deserialization of entities.
 *
 * @author Jake Wharton (jw@squareup.com)
 */
public class GsonConverter implements Converter {
  private static final MimeType JSON = new MimeType("application/json", "json");

  private final Gson gson;

  public GsonConverter(Gson gson) {
    this.gson = gson;
  }

  @Override public Object to(byte[] body, Type type) throws ConversionException {
    try {
      InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(body), UTF_8);
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
    private final byte[] jsonBytes;

    JsonTypedBytes(Gson gson, Object object) {
      try {
        jsonBytes = gson.toJson(object).getBytes(UTF_8);
      } catch (UnsupportedEncodingException e) {
        throw new IllegalStateException(UTF_8 + " encoding does not exist.");
      }
    }

    @Override public MimeType mimeType() {
      return JSON;
    }

    @Override public int length() {
      return jsonBytes.length;
    }

    @Override public void writeTo(OutputStream out) throws IOException {
      out.write(jsonBytes);
    }
  }
}
