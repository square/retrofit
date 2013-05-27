// Copyright 2013 Square, Inc.
package retrofit.converter;

import org.junit.Test;
import org.xml.sax.helpers.DefaultHandler;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedOutput;

import java.lang.reflect.Type;

import static org.fest.assertions.api.Assertions.assertThat;

public class SAXConverterTest {
  Converter converter = new TestSAXConverter();

  @Test public void successfulParse() throws Exception {
    String xml = "<Credentials>\n" +
        "\t<AccessKeyId>ACCESS</AccessKeyId>\n" +
        "\t<SecretAccessKey>SECRET</SecretAccessKey>\n" +
        "</Credentials>";

    TypedByteArray body = new TypedByteArray("UTF-8", xml.getBytes("UTF-8"));
    SessionCredentials credentials = (SessionCredentials) converter.fromBody(body, SessionCredentials.class);
    assertThat(credentials.accessKeyId).isEqualTo("ACCESS");
    assertThat(credentials.secretAccessKey).isEqualTo("SECRET");
    assertThat(credentials.sessionToken).isNull();
  }

  @Test(expected = ConversionException.class)
  public void malformedThrowsConversionException() throws Exception {
    TypedByteArray body = new TypedByteArray("UTF-8", "</Credentials>".getBytes("UTF-8"));
    converter.fromBody(body, SessionCredentials.class);
  }

  static class TestSAXConverter extends SAXConverter {
    @Override
    protected Deserializer newDeserializer(Type type) {
      return new SessionCredentialsHandler();
    }

    @Override
    public TypedOutput toBody(Object object) {
      throw new UnsupportedOperationException();
    }
  }

  static class SessionCredentialsHandler extends DefaultHandler implements SAXConverter.Deserializer {

    private StringBuilder currentText = new StringBuilder();
    private SessionCredentials credentials = new SessionCredentials();

    @Override
    public SessionCredentials getResult() {
      return credentials;
    }

    @Override
    public void endElement(String uri, String name, String qName) {
      if (qName.equals("AccessKeyId")) {
        credentials.accessKeyId = currentText.toString().trim();
      } else if (qName.equals("SecretAccessKey")) {
        credentials.secretAccessKey = currentText.toString().trim();
      } else if (qName.equals("SessionToken")) {
        credentials.sessionToken = currentText.toString().trim();
      }
      currentText = new StringBuilder();
    }

    @Override
    public void characters(char ch[], int start, int length) {
      currentText.append(ch, start, length);
    }
  }

  static class SessionCredentials {
    String accessKeyId;
    String secretAccessKey;
    String sessionToken;
  }
}
