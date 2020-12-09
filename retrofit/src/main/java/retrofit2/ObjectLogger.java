package retrofit2;

import okhttp3.Request;

public interface ObjectLogger {

  void log(Request request, Object obj);
}
