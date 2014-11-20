package retrofit.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;

import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.Serializer;

import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/**
 * A {@link Converter} which uses SimpleXML for reading and writing entities.
 *
 * @author Fabien Ric (fabien.ric@gmail.com)
 */
public class SimpleXMLConverter implements Converter {
  private static final boolean DEFAULT_STRICT = true;
  private static final String CHARSET = "UTF-8";
  private static final String MIME_TYPE = "application/xml; charset=" + CHARSET;

  private final Serializer serializer;

  private final boolean strict;

  public SimpleXMLConverter() {
    this(DEFAULT_STRICT);
  }

  public SimpleXMLConverter(boolean strict) {
    this(new Persister(), strict);
  }

  public SimpleXMLConverter(Serializer serializer) {
    this(serializer, DEFAULT_STRICT);
  }

  public SimpleXMLConverter(Serializer serializer, boolean strict) {
    this.serializer = serializer;
    this.strict = strict;
  }

  @Override public Object fromBody(TypedInput body, Type type) throws ConversionException {
    try {
      return serializer.read((Class<?>) type, body.in(), strict);
    } catch (Exception e) {
      throw new ConversionException(e);
    }
  }

  @Override public TypedOutput toBody(Object source) {
    OutputStreamWriter osw = null;

    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      osw = new OutputStreamWriter(bos, CHARSET);
      serializer.write(source, osw);
      osw.flush();
      return new TypedByteArray(MIME_TYPE, bos.toByteArray());
    } catch (Exception e) {
      throw new AssertionError(e);
    } finally {
      try {
        if (osw != null) {
          osw.close();
        }
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }
  }

  public boolean isStrict() {
    return strict;
  }
}
