package retrofit2.converter.thrifty;

import com.microsoft.thrifty.Adapter;
import com.microsoft.thrifty.StructBuilder;
import com.microsoft.thrifty.protocol.Protocol;
import com.microsoft.thrifty.transport.BufferTransport;
import com.microsoft.thrifty.transport.Transport;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import retrofit2.Converter;

final class ThriftyRequestBodyConverter<T> implements Converter<T, RequestBody> {

  private static final MediaType MEDIA_TYPE = MediaType.parse("application/x-thrift");

  private final ProtocolType protocolType;

  private final Adapter<T, StructBuilder<T>> adapter;

  ThriftyRequestBodyConverter(ProtocolType protocolType, Adapter<T, StructBuilder<T>> adapter) {
    this.protocolType = protocolType;
    this.adapter = adapter;
  }

  @Override
  public RequestBody convert(T value) throws IOException {
    Buffer buffer = new Buffer();
    Transport transport = new BufferTransport(buffer);
    Protocol protocol = ProtocolType.createProtocol(protocolType, transport);
    adapter.write(protocol, value);
    return RequestBody.create(MEDIA_TYPE, buffer.snapshot());
  }
}
