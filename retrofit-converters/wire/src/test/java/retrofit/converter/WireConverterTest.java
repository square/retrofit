// Copyright 2013 Square, Inc.
package retrofit.converter;

import com.google.common.io.BaseEncoding;
import com.squareup.wire.Wire;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import org.junit.Test;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedOutput;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class WireConverterTest {
  private static final Person PROTO =
      new Person.Builder().id(42).name("Omar Little").email("omar@theking.org").build();
  private static final String PROTO_ENCODED = "CgtPbWFyIExpdHRsZRAqGhBvbWFyQHRoZWtpbmcub3Jn";

  private WireConverter converter = new WireConverter(new Wire());

  @Test public void serialize() throws Exception {
    TypedOutput protoBytes = converter.toBody(PROTO, Person.class);
    assertThat(protoBytes.mimeType()).isEqualTo("application/x-protobuf");
    assertThat(bytesOf(protoBytes)).isEqualTo(bytesOf(decodeBase64(PROTO_ENCODED)));
  }

  @Test public void deserialize() throws Exception {
    Object proto = converter.fromBody(decodeBase64(PROTO_ENCODED), Person.class);
    assertThat(proto).isEqualTo(PROTO);
  }

  @Test public void deserializeWrongClass() throws Exception {
    try {
      converter.fromBody(decodeBase64(PROTO_ENCODED), String.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Expected a proto message but was java.lang.String");
    }
  }

  @Test public void deserializeWrongType() throws Exception {
    try {
      converter.fromBody(decodeBase64(PROTO_ENCODED),
          ArrayList.class.getGenericSuperclass());
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Expected a raw Class<?> but was java.util.AbstractList<E>");
    }
  }

  @Test public void deserializeWrongValue() throws Exception {
    try {
      converter.fromBody(decodeBase64("////"), Person.class);
      fail();
    } catch (ConversionException expected) {
      assertThat(expected.getCause()).isInstanceOf(IOException.class);
    }
  }

  @Test public void deserializeWrongMime() throws Exception {
    try {
      converter.fromBody(decodeBase64("////", "yummy/bytes"), Person.class);
      fail();
    } catch (ConversionException e) {
      assertThat(e).hasMessage("Expected a proto but was: yummy/bytes");
    }
  }

  private static byte[] bytesOf(TypedOutput protoBytes) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    protoBytes.writeTo(bytes);
    return bytes.toByteArray();
  }

  private static TypedByteArray decodeBase64(String base64) throws UnsupportedEncodingException {
    return decodeBase64(base64, "application/x-protobuf");
  }

  private static TypedByteArray decodeBase64(String base64, String mime) throws UnsupportedEncodingException {
    return new TypedByteArray(mime, BaseEncoding.base64().decode(base64));
  }
}
