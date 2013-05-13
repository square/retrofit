// Copyright 2013 Square, Inc.
package retrofit;

import java.lang.reflect.Method;
import java.util.Map;
import retrofit.mime.MultipartTypedOutput;
import retrofit.mime.TypedOutput;

import static org.fest.assertions.api.Assertions.assertThat;

public abstract class TestingUtils {
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

  public static void assertMultipart(TypedOutput typedOutput) {
    assertThat(typedOutput).isInstanceOf(MultipartTypedOutput.class);
  }

  public static void assertBytes(byte[] bytes, String expected) throws Exception {
    assertThat(new String(bytes, "UTF-8")).isEqualTo(expected);
  }
}
