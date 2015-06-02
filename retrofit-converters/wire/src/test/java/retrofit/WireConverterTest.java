// Copyright 2013 Square, Inc.
package retrofit;

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

public final class WireConverterTest {
  private static final Person PROTO =
      new Person.Builder().id(42).name("Omar Little").email("omar@theking.org").build();
  private static final String ENCODED_PROTO = "CgtPbWFyIExpdHRsZRAqGhBvbWFyQHRoZWtpbmcub3Jn";

  private final WireConverter converter = new WireConverter();

  @Test public void serialize() throws Exception {
    RequestBody body = converter.toBody(PROTO, Person.class);
    assertThat(body.contentType().toString()).isEqualTo("application/x-protobuf");
    assertBody(body).isEqualTo(ENCODED_PROTO);
  }

  @Test public void deserialize() throws Exception {
    Object proto = converter.fromBody(protoResponse(ENCODED_PROTO), Person.class);
    assertThat(proto).isEqualTo(PROTO);
  }

  @Test public void deserializeWrongClass() throws Exception {
    try {
      converter.fromBody(protoResponse(ENCODED_PROTO), String.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Expected a proto message but was java.lang.String");
    }
  }

  @Test public void deserializeWrongType() throws Exception {
    try {
      converter.fromBody(protoResponse(ENCODED_PROTO),
          ArrayList.class.getGenericSuperclass());
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Expected a raw Class<?> but was java.util.AbstractList<E>");
    }
  }

  @Test public void deserializeWrongValue() throws Exception {
    try {
      converter.fromBody(protoResponse("////"), Person.class);
      fail();
    } catch (IOException ignored) {
    }
  }

  private static ResponseBody protoResponse(String encodedProto) {
    return ResponseBody.create(MediaType.parse("application/x-protobuf"),
        ByteString.decodeBase64(encodedProto).toByteArray());
  }

  private static AbstractCharSequenceAssert<?, String> assertBody(RequestBody body) throws IOException {
    Buffer buffer = new Buffer();
    body.writeTo(buffer);
    return assertThat(buffer.readByteString().base64());
  }
}
