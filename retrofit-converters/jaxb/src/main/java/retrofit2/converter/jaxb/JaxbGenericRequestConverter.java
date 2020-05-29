package retrofit2.converter.jaxb;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;
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
  final Map<String, Object> marshalProps;
  final Class<T> type;

  protected JaxbGenericRequestConverter(
      JAXBContext context, MediaType contentType, Map<String, Object> marshalProps, Class<T> type) {
    this.context = context;
    this.contentType = contentType;
    this.marshalProps = marshalProps;
    this.type = type;
  }

  @Override
  public RequestBody convert(final T value) throws IOException {
    Buffer buffer = new Buffer();
    Charset charset = contentType.charset();
    try (OutputStreamWriter osw =
        new OutputStreamWriter(buffer.outputStream(), charset != null ? charset.name() : "utf-8")) {
      Marshaller marshaller = createMarshaller();
      marshaller.marshal(value, osw);
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
    return RequestBody.create(contentType, buffer.readByteString());
  }

  private Marshaller createMarshaller() throws JAXBException {
    Marshaller marshaller = context.createMarshaller();
    if (!marshalProps.isEmpty()) {
      for (Map.Entry<String, Object> entry : marshalProps.entrySet()) {
        marshaller.setProperty(entry.getKey(), entry.getValue());
      }
    }
    return marshaller;
  }
}
