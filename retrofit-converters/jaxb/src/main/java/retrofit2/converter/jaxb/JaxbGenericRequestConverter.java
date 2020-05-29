package retrofit2.converter.jaxb;

import java.io.*;
import java.nio.charset.Charset;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import retrofit2.Converter;

final class JaxbGenericRequestConverter<T> implements Converter<T, RequestBody> {
  final JAXBContext context;
  final MediaType contentType;
  final Class<T> type;

  protected JaxbGenericRequestConverter(JAXBContext context, MediaType contentType, Class<T> type) {
    this.context = context;
    this.contentType = contentType;
    this.type = type;
  }

  @Override
  public RequestBody convert(final T value) throws IOException {
    Buffer buffer = new Buffer();
    Charset charset = contentType.charset();
    try (OutputStreamWriter osw =
        new OutputStreamWriter(buffer.outputStream(), charset != null ? charset.name() : "utf-8")) {
      Marshaller marshaller = context.createMarshaller();
      marshaller.marshal(value, osw);
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
    return RequestBody.create(contentType, buffer.readByteString());
  }
}
