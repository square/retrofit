package retrofit;

import com.squareup.okhttp.Request;

public interface RequestFactory {
  Request create(Object... args);
}
