package retrofit2.converter.jaxb;

import java.io.IOException;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import okhttp3.ResponseBody;
import retrofit2.Converter;

final class JaxbGenericResponseConverter<T> implements Converter<ResponseBody, T> {
  final JAXBContext context;
  final Map<String, Object> unmarshalProps;
  final Class<T> type;

  protected JaxbGenericResponseConverter(
      JAXBContext context, Map<String, Object> unmarshalProps, Class<T> type) {
    this.context = context;
    this.unmarshalProps = unmarshalProps;
    this.type = type;
  }

  @Override
  public T convert(ResponseBody value) throws IOException {
    try {
      Source source = new StreamSource(value.charStream());
      Unmarshaller unmarshaller = createUnmarshaller();
      return unmarshaller.unmarshal(source, type).getValue();
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    } finally {
      value.close();
    }
  }

  private Unmarshaller createUnmarshaller() throws JAXBException {
    Unmarshaller unmarshaller = context.createUnmarshaller();
    if (!unmarshalProps.isEmpty()) {
      for (Map.Entry<String, Object> entry : unmarshalProps.entrySet()) {
        unmarshaller.setProperty(entry.getKey(), entry.getValue());
      }
    }
    return unmarshaller;
  }
}
