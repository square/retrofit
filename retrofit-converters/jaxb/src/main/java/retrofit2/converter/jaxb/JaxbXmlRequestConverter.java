/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit2.converter.jaxb;

import java.io.IOException;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import okhttp3.RequestBody;
import okio.Buffer;
import retrofit2.Converter;

final class JaxbXmlRequestConverter<T> implements Converter<T, RequestBody> {
  final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
  final JAXBContext context;
  final Map<String, Object> marshalProps;
  final Class<T> type;

  protected JaxbXmlRequestConverter(JAXBContext context, Map<String, Object> marshalProps, Class<T> type) {
    this.context = context;
    this.marshalProps = marshalProps;
    this.type = type;
  }

  @Override
  public RequestBody convert(final T value) throws IOException {
    Buffer buffer = new Buffer();
    try {
      Marshaller marshaller = createMarshaller();

      XMLStreamWriter xmlWriter =
          xmlOutputFactory.createXMLStreamWriter(
              buffer.outputStream(), JaxbConverterFactory.XML.charset().name());
      marshaller.marshal(value, xmlWriter);
    } catch (JAXBException | XMLStreamException e) {
      throw new RuntimeException(e);
    }
    return RequestBody.create(JaxbConverterFactory.XML, buffer.readByteString());
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
