package retrofit;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.lang.reflect.Type;

class StringConverter implements Converter {
  @Override public Object fromBody(ResponseBody body, Type type) throws IOException {
    return body.string();
  }

  @Override public RequestBody toBody(Object object, Type type) {
    return RequestBody.create(MediaType.parse("text/plain"), String.valueOf(object));
  }
}
