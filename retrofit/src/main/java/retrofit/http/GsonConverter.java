// Copyright 2012 Square, Inc.
package retrofit.http;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import retrofit.http.mime.TypedInput;
import retrofit.http.mime.TypedOutput;

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

  @Override public Object fromBody(TypedInput body, Type type) throws ConversionException {
    String charset = Utils.parseCharset(body.mimeType());
    InputStreamReader isr = null;
    try {
      isr = new InputStreamReader(body.in(), charset);
      return gson.fromJson(isr, type);
    } catch (IOException e) {
      throw new ConversionException(e);
    } catch (JsonParseException e) {
      throw new ConversionException(e);
    } finally {
      if (isr != null) {
        try {
          isr.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

  @Override public TypedOutput toBody(Object object) {
    try {
      return new JsonTypedOutput(gson.toJson(object).getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  static class JsonTypedOutput implements TypedOutput {
    private final byte[] jsonBytes;

    JsonTypedOutput(byte[] jsonBytes) {
      this.jsonBytes = jsonBytes;
    }

    @Override public String mimeType() {
      return "application/json; charset=UTF-8";
    }

    @Override public long length() {
      return jsonBytes.length;
    }

    @Override public void writeTo(OutputStream out) throws IOException {
      out.write(jsonBytes);
    }
  }
}
