package retrofit.converter;

import com.google.common.base.Objects;
import com.google.common.io.ByteStreams;
import com.google.common.reflect.TypeToken;
import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
import com.google.protobuf.nano.MessageNano;

import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

import java.io.IOException;
import java.lang.reflect.Type;

/** A {@link Converter} that reads and writes Nano Protocol Buffers. */
public class NanoProtoConverter implements Converter {
  private static final String MIME_TYPE = "application/x-protobuf";

  @Override
  public Object fromBody(final TypedInput body, final Type type) throws ConversionException {
    if (!Objects.equal(MIME_TYPE, body.mimeType())) {
      throw new ConversionException("Response content type was not a proto: " + body.mimeType());
    }

    try {
      return MessageNano.mergeFrom(getNanoProtoInstance(type), getMessageByteArray(body));
    } catch (InvalidProtocolBufferNanoException e) {
      throw new ConversionException("Nanoproto conversion failed", e);
    }
  }

  private MessageNano getNanoProtoInstance(final Type type) throws ConversionException {
    final TypeToken typeToken = TypeToken.of(type);
    if (!TypeToken.of(MessageNano.class).isAssignableFrom(typeToken)) {
      throw new IllegalArgumentException(
          "Expected a nanoproto message but was " + typeToken.toString());
    }

    try {
      return (MessageNano) typeToken.getRawType().newInstance();
    } catch (InstantiationException e) {
      throw new ConversionException("Nanoproto instantiation failed", e);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  private byte[] getMessageByteArray(final TypedInput body) throws ConversionException {
    try {
      return ByteStreams.toByteArray(body.in());
    } catch (IOException e) {
      throw new ConversionException("Reading nanoproto failed", e);
    }
  }

  @Override
  public TypedOutput toBody(final Object object) {
    if (!(object instanceof MessageNano)) {
     throw new IllegalArgumentException(
         "Expected a nanoproto but was " + object.getClass().getName());
    }
    return new TypedByteArray(MIME_TYPE, MessageNano.toByteArray((MessageNano) object));
  }

}
