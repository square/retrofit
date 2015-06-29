package retrofit;

import java.io.IOException;

public interface WebSocket<M> {
  void send(M message) throws IOException;
}
