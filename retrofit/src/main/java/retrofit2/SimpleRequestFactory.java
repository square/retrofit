package retrofit2;

import java.io.IOException;

public interface SimpleRequestFactory {
  okhttp3.Request create(Object[] args) throws IOException;
}
