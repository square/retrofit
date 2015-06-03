package retrofit;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import okio.Buffer;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * A {@link Converter} which uses SimpleXML for reading and writing entities.
 *
 * @author Fabien Ric (fabien.ric@gmail.com)
 */
public class SimpleXmlConverter implements Converter {
  private static final boolean DEFAULT_STRICT = true;
  private static final String CHARSET = "UTF-8";
  private static final MediaType MEDIA_TYPE =
      MediaType.parse("application/xml; charset=" + CHARSET);

  private final Serializer serializer;

  private final boolean strict;

  public SimpleXmlConverter() {
    this(DEFAULT_STRICT);
  }

  public SimpleXmlConverter(boolean strict) {
    this(new Persister(), strict);
  }

  public SimpleXmlConverter(Serializer serializer) {
    this(serializer, DEFAULT_STRICT);
  }

  public SimpleXmlConverter(Serializer serializer, boolean strict) {
    this.serializer = serializer;
    this.strict = strict;
  }

  @Override public Object fromBody(ResponseBody body, Type type) throws IOException {
    InputStream is = body.byteStream();
    try {
      return serializer.read((Class<?>) type, is, strict);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
        is.close();
      } catch (IOException ignored) {
      }
    }
  }

  @Override public RequestBody toBody(Object source, Type type) {
    byte[] bytes;
    try {
      Buffer buffer = new Buffer();
      OutputStreamWriter osw = new OutputStreamWriter(buffer.outputStream(), CHARSET);
      serializer.write(source, osw);
      osw.flush();
      bytes = buffer.readByteArray();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
    return RequestBody.create(MEDIA_TYPE, bytes);
  }

  public boolean isStrict() {
    return strict;
  }
}
