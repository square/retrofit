package retrofit.http;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import retrofit.core.Callback;
import retrofit.internal.gson.JsonParseException;

import static retrofit.http.GsonProvider.gson;

/**
 * Converts JSON response to an object using Gson and then passes it to {@link
 * Callback#call(T)}.
 */
class GsonResponseHandler<T> extends CallbackResponseHandler<T> {
  private static final Logger logger =
    Logger.getLogger(GsonResponseHandler.class.getName());

  private final Type type;

  private GsonResponseHandler(Type type, Callback<T> callback) {
    super(callback);
    this.type = type;
  }

  static <T> GsonResponseHandler<T> create(Type type,
      Callback<T> callback) {
    return new GsonResponseHandler<T>(type, callback);
  }

  @Override protected T parse(HttpEntity entity) throws IOException,
      ServerException {
    try {
      if (logger.isLoggable(Level.FINE)) {
        entity = HttpClients.copyAndLog(entity);
      }

      // TODO: Use specified encoding.
      InputStreamReader in = new InputStreamReader(entity.getContent(),
          "UTF-8");

      /*
       * It technically isn't safe for fromJson() to return T here.
       * We derived type from Callback<T>, so we know we're safe.
       */
      @SuppressWarnings("unchecked")
      T t = (T) gson().fromJson(in, type);
      return t;
    } catch (JsonParseException e) {
      // The server returned us bad JSON!
      throw new ServerException(e);
    }
  }
}