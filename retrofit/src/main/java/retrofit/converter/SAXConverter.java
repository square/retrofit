/*
 * Copyright (C) 2012 Square, Inc.
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
package retrofit.converter;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import retrofit.mime.MimeUtil;
import retrofit.mime.TypedInput;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

/**
 * A {@link retrofit.converter.Converter} which uses SAX (XML) to deserialize entities.
 *
 * @author Adrian Cole (adrianc@netflix.com)
 */
public abstract class SAXConverter implements Converter {

  private final SAXParserFactory factory;

  protected SAXConverter() {
    factory = SAXParserFactory.newInstance();
    factory.setNamespaceAware(false);
    factory.setValidating(false);
  }

  protected SAXConverter(SAXParserFactory factory) {
    this.factory = factory;
  }

  @Override
  public Object fromBody(TypedInput body, Type type) throws ConversionException {
    String charset = MimeUtil.parseCharset(body.mimeType());
    Deserializer deserializer = newDeserializer(type);
    InputStreamReader isr = null;
    try {
      XMLReader reader = factory.newSAXParser().getXMLReader();
      reader.setContentHandler(deserializer);
      isr = new InputStreamReader(body.in(), charset);
      InputSource source = new InputSource(isr);
      source.setEncoding(charset);
      reader.parse(source);
      return deserializer.getResult();
    } catch (IOException e) {
      throw new ConversionException(e);
    } catch (ParserConfigurationException e) {
      throw new ConversionException(e);
    } catch (SAXException e) {
      throw new ConversionException(e);
    } finally {
      if (isr != null) {
        try {
          isr.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

  protected abstract Deserializer newDeserializer(Type type);

  public interface Deserializer extends ContentHandler {
    Object getResult();
  }
}
