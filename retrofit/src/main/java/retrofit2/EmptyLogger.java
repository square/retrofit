package retrofit2;

import okhttp3.Request;

public class EmptyLogger implements ObjectLogger {

  @Override
  public void log(Request request, Object obj) {}
}
