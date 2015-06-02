package retrofit;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import okio.Buffer;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.Before;
import org.junit.Test;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.HyphenStyle;
import org.simpleframework.xml.stream.Verbosity;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleXmlConverterTest {
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/xml; charset=UTF-8");
  private static final MyObject OBJ = new MyObject("hello world", 10);
  private static final String XML =
      "<my-object><message>hello world</message><count>10</count></my-object>";

  private Converter converter;

  @Before public void setUp() {
    Format format = new Format(0, null, new HyphenStyle(), Verbosity.HIGH);
    Persister persister = new Persister(format);
    converter = new SimpleXmlConverter(persister);
  }

  @Test public void serialize() throws Exception {
    RequestBody body = converter.toBody(OBJ, MyObject.class);
    assertThat(body.contentType()).isEqualTo(MEDIA_TYPE);
    assertBody(body).isEqualTo(XML);
  }

  @Test public void deserialize() throws Exception {
    ResponseBody body = ResponseBody.create(MEDIA_TYPE, XML);
    MyObject result = (MyObject) converter.fromBody(body, MyObject.class);
    assertThat(result).isEqualTo(OBJ);
  }

  @Test public void deserializeWrongValue() throws Exception {
    ResponseBody body = ResponseBody.create(MEDIA_TYPE, "<myObject><foo/><bar/></myObject>");
    try {
      converter.fromBody(body, MyObject.class);
    } catch (RuntimeException ignored) {
    }
  }

  @Test public void deserializeWrongClass() throws Exception {
    ResponseBody body = ResponseBody.create(MEDIA_TYPE, XML);
    Object result = converter.fromBody(body, String.class);
    assertThat(result).isNull();
  }

  private static AbstractCharSequenceAssert<?, String> assertBody(RequestBody body)
      throws IOException {
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    return assertThat(buffer.readUtf8());
  }

  @Default(value = DefaultType.FIELD) static class MyObject {
    @Element private String message;
    @Element private int count;

    public MyObject() {
    }

    public MyObject(String message, int count) {
      this.message = message;
      this.count = count;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public String getMessage() {
      return message;
    }

    public void setCount(int count) {
      this.count = count;
    }

    public int getCount() {
      return count;
    }

    @Override public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + count;
      result = prime * result + ((message == null) ? 0 : message.hashCode());
      return result;
    }

    @Override public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      MyObject other = (MyObject) obj;
      if (count != other.count) {
        return false;
      }
      if (message == null) {
        if (other.message != null) {
          return false;
        }
      } else if (!message.equals(other.message)) {
        return false;
      }
      return true;
    }
  }
}
