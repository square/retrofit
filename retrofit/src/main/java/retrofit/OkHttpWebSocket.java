package retrofit;

import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ws.WebSocket.PayloadType;
import java.io.IOException;
import okio.BufferedSink;

final class OkHttpWebSocket<M> implements WebSocket<M> {
  private final com.squareup.okhttp.ws.WebSocket rawWebSocket;
  private final Converter<M> converter;

  public OkHttpWebSocket(com.squareup.okhttp.ws.WebSocket rawWebSocket, Converter<M> converter) {
    this.rawWebSocket = rawWebSocket;
    this.converter = converter;
  }

  @Override public void send(M message) throws IOException {
    RequestBody body = converter.toBody(message);
    BufferedSink sink = rawWebSocket.newMessageSink(PayloadType.TEXT);
    try {
      body.writeTo(sink);
    } finally {
      Utils.closeQuietly(sink);
    }
  }
}
