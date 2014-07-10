package retrofit.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

import javax.activation.DataSource;
import javax.mail.internet.MimeMultipart;

import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

import com.google.gson.Gson;

/**
 * A converter to handle multipart/mixed response from Web Services. <br>
 * Usage:
 * <pre>
 * new RestAdapter.Builder()
 * .setEndpoint(ENDPOINT_ADDRESS)
 * .setConverter(new MultipartConverter()).build();
 * @POST("/documents")
 * public MimeMultipart getDocumentsAndContent(...);
 * </pre>
 * @see {@link http://www.w3.org/Protocols/rfc1341/7_2_Multipart.html}
 */
public class MultipartConverter implements Converter {

    private final Converter delegate = new GsonConverter(new Gson());

    @Override
    public Object fromBody(TypedInput body, Type type)
            throws ConversionException {
        if (type.equals(MimeMultipart.class)) {
            try {
                MimeMultipart m = new MimeMultipart(new InputStreamDataSource(
                        body.in(), "multipart/mixed"));
                return m;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            return delegate.fromBody(body, type);
        }
    }

    @Override
    public TypedOutput toBody(Object object) {
        return delegate.toBody(object);
    }

    private class InputStreamDataSource implements DataSource {
        private InputStream is;
        private String contentType;
        private String name;

        public InputStreamDataSource(InputStream is, String contentType) {
            this.is = is;
            this.contentType = contentType;
        }

        public String getContentType() {
            return contentType;
        }

        public InputStream getInputStream() throws IOException {
            return is;
        }

        public String getName() {
            return name;
        }

        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}
