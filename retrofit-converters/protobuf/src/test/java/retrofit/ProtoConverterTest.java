// Copyright 2013 Square, Inc.
package retrofit;

import com.google.protobuf.InvalidProtocolBufferException;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.util.ArrayList;
import okio.Buffer;
import okio.ByteString;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static retrofit.PhoneProtos.Phone;

public final class ProtoConverterTest {
  private static final Phone PROTO = Phone.newBuilder().setNumber("(519) 867-5309").build();
  private static final String ENCODED_PROTO = "Cg4oNTE5KSA4NjctNTMwOQ==";

  private final ProtoConverter converter = new ProtoConverter();

  @Test public void serialize() throws Exception {
    RequestBody body = converter.toBody(PROTO, Phone.class);
    assertThat(body.contentType().toString()).isEqualTo("application/x-protobuf");
    assertBody(body).isEqualTo(ENCODED_PROTO);
  }

  @Test public void deserialize() throws Exception {
    Object proto = converter.fromBody(protoResponse(ENCODED_PROTO), Phone.class);
    assertThat(proto).isEqualTo(PROTO);
  }

  @Test public void deserializeWrongClass() throws Exception {
    try {
      converter.fromBody(protoResponse(ENCODED_PROTO), String.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Expected a protobuf message but was java.lang.String");
    }
  }

  @Test public void deserializeWrongType() throws Exception {
    try {
      converter.fromBody(protoResponse(ENCODED_PROTO), ArrayList.class.getGenericSuperclass());
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Expected a raw Class<?> but was java.util.AbstractList<E>");
    }
  }

  @Test public void deserializeWrongValue() throws Exception {
    try {
      converter.fromBody(protoResponse("////"), Phone.class);
      fail();
    } catch (RuntimeException expected) {
      assertThat(expected.getCause() instanceof InvalidProtocolBufferException);
    }
  }

  private static ResponseBody protoResponse(String encodedProto) {
    return ResponseBody.create(MediaType.parse("application/x-protobuf"), ByteString.decodeBase64(
        encodedProto).toByteArray());
  }

  private static AbstractCharSequenceAssert<?, String> assertBody(RequestBody body) throws IOException {
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    return assertThat(buffer.readByteString().base64());
  }
}
