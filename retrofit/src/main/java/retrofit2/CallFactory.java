package retrofit2;

import java.io.IOException;

import okhttp3.ResponseBody;

public interface CallFactory<T> {
  okhttp3.Call create(Object... args) throws IOException;
  T toResponse(ResponseBody body) throws IOException;
}