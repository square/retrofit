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
import retrofit.io.TypedBytes;

import static retrofit.http.RestAdapter.UTF_8;

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

  @Override public Object fromBody(byte[] body, Type type) throws ConversionException {
    try {
      InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(body), UTF_8);
      return gson.fromJson(isr, type);
    } catch (IOException e) {
      throw new ConversionException(e);
    } catch (JsonParseException e) {
      throw new ConversionException(e);
    }
  }

  @Override public TypedBytes toBody(Object object) {
    try {
      return new JsonTypedBytes(gson.toJson(object).getBytes(UTF_8));
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  static class JsonTypedBytes implements TypedBytes {
    final byte[] jsonBytes;

    JsonTypedBytes(byte[] jsonBytes) {
      this.jsonBytes = jsonBytes;
    }

    @Override public String mimeType() {
      return "application/json; charset=UTF-8";
    }

    @Override public int length() {
      return jsonBytes.length;
    }

    @Override public void writeTo(OutputStream out) throws IOException {
      out.write(jsonBytes);
    }
  }
}
