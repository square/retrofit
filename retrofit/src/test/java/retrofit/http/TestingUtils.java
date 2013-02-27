// Copyright 2013 Square, Inc.
package retrofit.http;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import org.apache.http.entity.mime.HttpMultipart;
import org.apache.http.entity.mime.MultipartEntity;
import retrofit.http.mime.TypedOutput;

import static org.fest.assertions.api.Assertions.assertThat;

public class TestingUtils {
  public static Method getMethod(Class c, String name) {
    for (Method method : c.getDeclaredMethods()) {
      if (method.getName().equals(name)) {
        return method;
      }
    }
    throw new IllegalArgumentException("Unknown method '" + name + "' on " + c);
  }

  public static TypedOutput createMultipart(Map<String, TypedOutput> parts) {
    MultipartTypedOutput typedOutput = new MultipartTypedOutput();
    for (Map.Entry<String, TypedOutput> part : parts.entrySet()) {
      typedOutput.addPart(part.getKey(), part.getValue());
    }
    return typedOutput;
  }

  public static HttpMultipart extractEntity(TypedOutput output)
      throws NoSuchFieldException, IllegalAccessException {
    if (!(output instanceof MultipartTypedOutput)) {
      throw new IllegalArgumentException("TypedOutput was not a MultipartTypedOutput.");
    }
    MultipartEntity entity = ((MultipartTypedOutput) output).cheat;
    Field httpMultipartField = MultipartEntity.class.getDeclaredField("multipart");
    httpMultipartField.setAccessible(true);
    return (HttpMultipart) httpMultipartField.get(entity);
  }

  public static void assertMultipart(TypedOutput typedOutput) {
    assertThat(typedOutput).isInstanceOf(MultipartTypedOutput.class);
  }

  public static void assertBytes(byte[] bytes, String expected) throws Exception {
    assertThat(new String(bytes, "UTF-8")).isEqualTo(expected);
  }
}
