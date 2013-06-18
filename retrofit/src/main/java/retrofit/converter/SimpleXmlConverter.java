package retrofit.converter;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import retrofit.mime.MimeUtil;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

/**
 * A {@link Converter} which uses Simple XML for serialization and deserialization of entities.
 *
 * @see <a href="http://simple.sourceforge.net/">http://simple.sourceforge.net/</a>
 *
 * @author Sebastian Engel (engel.sebastian@gmail.com)
 */
public class SimpleXmlConverter implements Converter {

    private Serializer serializer;

    public SimpleXmlConverter() {
        this.serializer = new Persister();
    }

    public SimpleXmlConverter(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public Object fromBody(TypedInput body, Type type) throws ConversionException {
        String charset = "UTF-8";
        if (body.mimeType() != null) {
            charset = MimeUtil.parseCharset(body.mimeType());
        }

        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(body.in(), charset);

            // Need a Class instance here, as using the Type in serializer.read(...) doesn't work
            Class<?> typeClass = (Class<?>) type;

            return serializer.read((Class<?>) type, isr);
        } catch (Exception e) {
            throw new ConversionException(e);
        }
    }

    @Override
    public TypedOutput toBody(Object object) {
        StringWriter stringWriter = new StringWriter();
        try {
            serializer.write(object, stringWriter);
            return new XmlTypedOutput(stringWriter.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private static class XmlTypedOutput implements TypedOutput {

        private final byte[] xmlBytes;

        XmlTypedOutput(byte[] xmlBytes) {
            this.xmlBytes = xmlBytes;
        }

        @Override
        public String fileName() {
            return null;
        }

        @Override
        public String mimeType() {
            return "application/xml; charset=UTF-8";
        }

        @Override
        public long length() {
            return xmlBytes.length;
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            out.write(xmlBytes);
        }

    }

}
