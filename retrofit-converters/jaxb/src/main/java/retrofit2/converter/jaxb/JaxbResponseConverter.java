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
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import okhttp3.ResponseBody;
import retrofit2.Converter;

final class JaxbResponseConverter<T> implements Converter<ResponseBody, T> {
  final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
  final JAXBContext context;
  final Class<T> type;
  final Map<String, Object> unmarshallerProperties;

  JaxbResponseConverter(JAXBContext context, Class<T> type, Map<String, Object> unmarshallerProperties) {
    this.context = context;
    this.type = type;
    this.unmarshallerProperties = unmarshallerProperties;

    // Prevent XML External Entity attacks (XXE).
    xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
  }

  @Override public T convert(ResponseBody value) throws IOException {
    try {
      Unmarshaller unmarshaller = context.createUnmarshaller();
      if (!unmarshallerProperties.isEmpty()) {
        Set<String> keys = unmarshallerProperties.keySet();
        for (String key : keys) {
          unmarshaller.setProperty(key, unmarshallerProperties.get(key));
        }
      }

      XMLStreamReader streamReader = xmlInputFactory.createXMLStreamReader(value.charStream());
      return unmarshaller.unmarshal(streamReader, type).getValue();
    } catch (JAXBException | XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }
}
