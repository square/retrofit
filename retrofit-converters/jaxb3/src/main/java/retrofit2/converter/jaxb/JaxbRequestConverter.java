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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.IOException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import okhttp3.RequestBody;
import okio.Buffer;
import retrofit2.Converter;

final class JaxbRequestConverter<T> implements Converter<T, RequestBody> {
  final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
  final JAXBContext context;
  final Class<T> type;

  JaxbRequestConverter(JAXBContext context, Class<T> type) {
    this.context = context;
    this.type = type;
  }

  @Override
  public RequestBody convert(final T value) throws IOException {
    Buffer buffer = new Buffer();
    try {
      Marshaller marshaller = context.createMarshaller();

      XMLStreamWriter xmlWriter =
          xmlOutputFactory.createXMLStreamWriter(
              buffer.outputStream(), JaxbConverterFactory.XML.charset().name());
      marshaller.marshal(value, xmlWriter);
    } catch (JAXBException | XMLStreamException e) {
      throw new RuntimeException(e);
    }
    return RequestBody.create(JaxbConverterFactory.XML, buffer.readByteString());
  }
}
