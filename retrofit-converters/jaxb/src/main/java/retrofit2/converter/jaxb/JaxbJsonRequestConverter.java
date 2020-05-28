package retrofit2.converter.jaxb;

import java.io.*;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import okhttp3.RequestBody;
import okio.Buffer;
import retrofit2.Converter;

final class JaxbJsonRequestConverter<T> implements Converter<T, RequestBody> {
  final JAXBContext context;
  final Map<String, Object> marshalProps;
  final Class<T> type;

  protected JaxbJsonRequestConverter(
      JAXBContext context, Map<String, Object> marshalProps, Class<T> type) {
    this.context = context;
    this.marshalProps = marshalProps;
    this.type = type;
  }

  @Override
  public RequestBody convert(final T value) throws IOException {
    Buffer buffer = new Buffer();
    try (OutputStreamWriter osw =
        new OutputStreamWriter(buffer.outputStream(), JaxbConverterFactory.JSON.charset().name())) {
      Marshaller marshaller = createMarshaller();
      marshaller.marshal(value, osw);
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
    return RequestBody.create(JaxbConverterFactory.JSON, buffer.readByteString());
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
