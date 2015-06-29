package retrofit;

public interface WebSocketCallback<I, O> {
  void connect(Response<WebSocket<O>> response);
  void message(I message);
  void failure(Throwable t);
}
