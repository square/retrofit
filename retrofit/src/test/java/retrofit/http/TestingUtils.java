// Copyright 2013 Square, Inc.
package retrofit.http;

import java.lang.reflect.Method;

public class TestingUtils {
  public static Method getMethod(Class c, String name) {
    for (Method method : c.getDeclaredMethods()) {
      if (method.getName().equals(name)) {
        return method;
      }
    }
    throw new IllegalArgumentException("Unknown method '" + name + "' on " + c);
  }
}
