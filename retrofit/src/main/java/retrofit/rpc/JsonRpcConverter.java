package retrofit.rpc;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;

import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.MimeUtil;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/**
 * Created by dp on 22/04/14.
 */
public class JsonRpcConverter implements Converter {

  private final Gson gson;
  private String encoding;

  /**
   * Create an instance using the supplied {@link Gson} object for conversion. Encoding to JSON and
   * decoding from JSON (when no charset is specified by a header) will use UTF-8.
   */
  public JsonRpcConverter(Gson gson) {
    this(gson, "UTF-8");
  }

  /**
   * Create an instance using the supplied {@link Gson} object for conversion. Encoding to JSON and
   * decoding from JSON (when no charset is specified by a header) will use the specified encoding.
   */
  public JsonRpcConverter(Gson gson, String encoding) {
    this.gson = gson;
    this.encoding = encoding;
  }

  @Override public Object fromBody(TypedInput body, Type type) throws ConversionException {
    String charset = "UTF-8";
    if (body.mimeType() != null) {
      charset = MimeUtil.parseCharset(body.mimeType());
    }
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
      return new JsonRpcTypedOutput(gson.toJson(object), encoding);
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  private static class JsonRpcTypedOutput implements TypedOutput {
    private final String data;
    private final String mimeType;
    private final String encoding;

    JsonRpcTypedOutput(String data, String encode) throws UnsupportedEncodingException {
      this.mimeType = "application/x-www-form-urlencoded";
      this.encoding = encode;
      this.data = "request=" + URLEncoder.encode(data, encoding);
    }

    @Override public String fileName() {
      return null;
    }

    @Override public String mimeType() {
      return mimeType;
    }

    @Override public long length() {
      return data.length();
    }

    @Override public void writeTo(OutputStream out) throws IOException {
      out.write(data.getBytes(encoding));
    }
  }
}
