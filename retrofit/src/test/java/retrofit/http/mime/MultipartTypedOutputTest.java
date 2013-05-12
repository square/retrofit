// Copyright 2013 Square, Inc.
package retrofit.http.mime;

import java.io.ByteArrayOutputStream;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class MultipartTypedOutputTest {
  @Test public void singlePart() throws Exception {
    String expected = "" //
        + "--123\r\n"
        + "Content-Disposition: form-data; name=\"greet\"\r\n"
        + "Content-Type: text/plain; charset=UTF-8\r\n"
        + "Content-Length: 13\r\n"
        + "Content-Transfer-Encoding: binary\r\n" //
        + "\r\n" //
        + "Hello, World!\r\n" //
        + "--123--";

    MultipartTypedOutput mto = new MultipartTypedOutput("123");
    mto.addPart("greet", new TypedString("Hello, World!"));

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    mto.writeTo(out);
    String actual = new String(out.toByteArray(), "UTF-8");
    assertThat(actual).isEqualTo(expected);
    assertThat(mto.mimeType()).isEqualTo("multipart/form-data; boundary=123");
  }

  @Test public void threeParts() throws Exception {
    String expected = ""
        + "--123\r\n"
        + "Content-Disposition: form-data; name=\"quick\"\r\n"
        + "Content-Type: text/plain; charset=UTF-8\r\n"
        + "Content-Length: 5\r\n"
        + "Content-Transfer-Encoding: binary\r\n"
        + "\r\n"
        + "brown\r\n"
        + "--123\r\n"
        + "Content-Disposition: form-data; name=\"fox\"\r\n"
        + "Content-Type: text/plain; charset=UTF-8\r\n"
        + "Content-Length: 5\r\n"
        + "Content-Transfer-Encoding: binary\r\n"
        + "\r\n"
        + "jumps\r\n"
        + "--123\r\n"
        + "Content-Disposition: form-data; name=\"lazy\"\r\n"
        + "Content-Type: text/plain; charset=UTF-8\r\n"
        + "Content-Length: 3\r\n"
        + "Content-Transfer-Encoding: binary\r\n"
        + "\r\n"
        + "dog\r\n"
        + "--123--";

    MultipartTypedOutput mto = new MultipartTypedOutput("123");
    mto.addPart("quick", new TypedString("brown"));
    mto.addPart("fox", new TypedString("jumps"));
    mto.addPart("lazy", new TypedString("dog"));

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    mto.writeTo(out);
    String actual = new String(out.toByteArray(), "UTF-8");
    assertThat(actual).isEqualTo(expected);
    assertThat(mto.mimeType()).isEqualTo("multipart/form-data; boundary=123");
  }
}
