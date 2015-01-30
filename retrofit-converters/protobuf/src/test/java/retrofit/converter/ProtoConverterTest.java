// Copyright 2013 Square, Inc.
package retrofit.converter;

import com.google.common.io.BaseEncoding;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import org.junit.Test;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedOutput;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static retrofit.converter.PhoneProtos.Phone;

public final class ProtoConverterTest {
  private static final Phone PROTO = Phone.newBuilder().setNumber("(519) 867-5309").build();
  private static final String ENCODED_PROTO = "Cg4oNTE5KSA4NjctNTMwOQ==";

  private final ProtoConverter protoConverter = new ProtoConverter();

  @Test public void serialize() throws Exception {
    TypedOutput protoBytes = protoConverter.toBody(PROTO, Phone.class);
    assertThat(protoBytes.mimeType()).isEqualTo("application/x-protobuf");
    assertThat(bytesOf(protoBytes)).isEqualTo(bytesOf(decodeBase64(ENCODED_PROTO)));
  }

  @Test public void deserialize() throws Exception {
    Object proto = protoConverter.fromBody(decodeBase64(ENCODED_PROTO), Phone.class);
    assertThat(proto).isEqualTo(PROTO);
  }

  @Test public void deserializeWrongClass() throws Exception {
    try {
      protoConverter.fromBody(decodeBase64(ENCODED_PROTO), String.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Expected a protobuf message but was java.lang.String");
    }
  }

  @Test public void deserializeWrongType() throws Exception {
    try {
      protoConverter.fromBody(decodeBase64(ENCODED_PROTO), ArrayList.class.getGenericSuperclass());
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Expected a raw Class<?> but was java.util.AbstractList<E>");
    }
  }

  @Test public void deserializeWrongValue() throws Exception {
    try {
      protoConverter.fromBody(decodeBase64("////"), Phone.class);
      fail();
    } catch (ConversionException expected) {
      assertThat(expected.getCause() instanceof InvalidProtocolBufferException);
    }
  }

  @Test public void deserializeWrongMime() throws Exception {
    try {
      protoConverter.fromBody(decodeBase64("////", "yummy/bytes"), Phone.class);
      fail();
    } catch (ConversionException e) {
      assertThat(e).hasMessage("Response content type was not a proto: yummy/bytes");
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
