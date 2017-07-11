package retrofit2.converter.thrifty;

import com.microsoft.thrifty.Adapter;
import com.microsoft.thrifty.StructBuilder;
import com.microsoft.thrifty.protocol.Protocol;
import com.microsoft.thrifty.transport.BufferTransport;
import com.microsoft.thrifty.transport.Transport;

import java.io.IOException;

import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Converter;

final class ThriftyResponseBodyConverter<T> implements Converter<ResponseBody, T> {

  private final ProtocolType protocolType;

  private final Adapter<T, StructBuilder<T>> adapter;

  ThriftyResponseBodyConverter(ProtocolType protocolType, Adapter<T, StructBuilder<T>> adapter) {
    this.protocolType = protocolType;
    this.adapter = adapter;
  }

  @Override
  public T convert(ResponseBody value) throws IOException {
    try {
      Buffer buffer = new Buffer();
      buffer.readFrom(value.byteStream());
      Transport transport = new BufferTransport(buffer);
      Protocol protocol = ProtocolType.createProtocol(protocolType, transport);
      return adapter.read(protocol);
    } finally {
      value.close();
    }
  }
}
