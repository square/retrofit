package retrofit2.converter.jaxb;

import java.io.IOException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import okhttp3.ResponseBody;
import retrofit2.Converter;

final class JaxbGenericResponseConverter<T> implements Converter<ResponseBody, T> {
  final JAXBContext context;
  final Class<T> type;

  protected JaxbGenericResponseConverter(JAXBContext context, Class<T> type) {
    this.context = context;
    this.type = type;
  }

  @Override
  public T convert(ResponseBody value) throws IOException {
    try {
      Source source = new StreamSource(value.charStream());
      Unmarshaller unmarshaller = context.createUnmarshaller();
      return unmarshaller.unmarshal(source, type).getValue();
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    } finally {
      value.close();
    }
  }
}
