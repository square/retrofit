package retrofit.http;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import retrofit.core.Callback;

/**
 * Converts JSON response to an object using Gson and then passes it to {@link
 * Callback#call(T)}.
 */
class GsonResponseHandler<T> extends CallbackResponseHandler<T> {
  private static final Logger LOGGER =
    Logger.getLogger(GsonResponseHandler.class.getName());

  private final Gson gson;
  private final Type type;
  private final String url;
  private final Date start;
  private ThreadLocal<SimpleDateFormat> dateFormat;

  private GsonResponseHandler(Gson gson, Type type, Callback<T> callback, String url, Date start,
      ThreadLocal<SimpleDateFormat> dateFormat) {
    super(gson, callback);
    this.gson = gson;
    this.type = type;
    this.url = url;
    this.start = start;
    this.dateFormat = dateFormat;
  }

  static <T> GsonResponseHandler<T> create(Gson gson, Type type, Callback<T> callback, String url,
      Date startTime, ThreadLocal<SimpleDateFormat> dateFormat) {
    return new GsonResponseHandler<T>(gson, type, callback, url, startTime, dateFormat);
  }

  @Override protected T parse(HttpEntity entity) throws IOException,
      ServerException {
    try {
      if (LOGGER.isLoggable(Level.FINE)) {
        entity = HttpClients.copyAndLog(entity, url, start, dateFormat.get());
      }

      // TODO: Use specified encoding.
      InputStreamReader in = new InputStreamReader(entity.getContent(),
          "UTF-8");

      /*
       * It technically isn't safe for fromJson() to return T here.
       * We derived type from Callback<T>, so we know we're safe.
       */
      @SuppressWarnings("unchecked")
      T t = (T) gson.fromJson(in, type);
      return t;
    } catch (JsonParseException e) {
      // The server returned us bad JSON!
      throw new ServerException(e);
    }
  }
}