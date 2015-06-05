package retrofit2;

import java.io.IOException;

import okhttp3.Request;
import okhttp3.ResponseBody;

class DefaultCallFactory<T> implements CallFactory<T> {
  private final ServiceMethod<T> serviceMethod;

  public DefaultCallFactory(ServiceMethod<T> serviceMethod) {
    this.serviceMethod = serviceMethod;
  }

  @Override public okhttp3.Call create(Object... args) throws IOException {
    Request request = serviceMethod.toRequest(args);
    return serviceMethod.callFactory.newCall(request);
  }

  @Override public T toResponse(ResponseBody body) throws IOException {
    return serviceMethod.toResponse(body);
  }
}
