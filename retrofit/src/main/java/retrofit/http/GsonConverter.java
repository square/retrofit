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

/**
 * A {@link Converter} which uses GSON for serialization and deserialization of entities.
 *
 * @author Jake Wharton (jw@squareup.com)
 */
public class GsonConverter implements Converter {
  private static final String ENCODING = "UTF-8"; // TODO use actual encoding
  private static final MimeType JSON = new MimeType("application/json", "json");

  private final Gson gson;

  public GsonConverter(Gson gson) {
    this.gson = gson;
  }

  @Override public Object to(byte[] body, Type type) throws ConversionException {
    try {
      InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(body), ENCODING);
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
        jsonBytes = gson.toJson(object).getBytes(ENCODING);
      } catch (UnsupportedEncodingException e) {
        throw new IllegalArgumentException(ENCODING + " doesn't exist!?");
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
