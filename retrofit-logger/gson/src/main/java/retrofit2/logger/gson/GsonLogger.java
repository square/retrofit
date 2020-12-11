package retrofit2.logger.gson;

import com.google.gson.Gson;
import okhttp3.Request;
import retrofit2.ObjectLogger;

public class GsonLogger implements ObjectLogger {
  private final Gson gson;

  public static GsonLogger create() {
    return create(new Gson());
  }

  @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
  public static GsonLogger create(Gson gson) {
    if (gson == null) throw new NullPointerException("gson == null");
    return new GsonLogger(gson);
  }

  private GsonLogger(Gson gson) {
    this.gson = gson;
  }

  @Override
  public void log(Request request, Object obj) {
    System.out.println("Request = ");
    System.out.println(request.url().toString());
    System.out.println("Response = ");
    System.out.println(gson.toJson(obj));
  }
}
