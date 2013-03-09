// Copyright 2012 Square, Inc.
// Copyright 2007 The Guava Authors
package retrofit.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public final class Utils {
  private static final Pattern CHARSET = Pattern.compile("\\Wcharset=([^\\s;]+)", CASE_INSENSITIVE);
  private static final int BUFFER_SIZE = 0x1000;

  /**
   * Creates a {@code byte[]} from reading the entirety of an {@link InputStream}. May return an
   * empty array but never {@code null}.
   * <p>
   * Copied from Guava's {@code ByteStreams} class.
   */
  static byte[] streamToBytes(InputStream stream) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    if (stream != null) {
      byte[] buf = new byte[BUFFER_SIZE];
      int r;
      while ((r = stream.read(buf)) != -1) {
        baos.write(buf, 0, r);
      }
    }
    return baos.toByteArray();
  }

  /**
   * Returns the generic supertype for {@code supertype}. For example, given a class
   * {@code IntegerSet}, the result for when supertype is {@code Set.class} is {@code Set<Integer>}
   * and the result when the supertype is {@code Collection.class} is {@code Collection<Integer>}.
   */
  // Copied from Guice's {@code MoreTypes} class. Copyright 2006 Google, Inc.
  static Type getGenericSupertype(Type context, Class<?> rawType, Class<?> toResolve) {
    if (toResolve == rawType) {
      return context;
    }

    // we skip searching through interfaces if unknown is an interface
    if (toResolve.isInterface()) {
      Class<?>[] interfaces = rawType.getInterfaces();
      for (int i = 0, length = interfaces.length; i < length; i++) {
        if (interfaces[i] == toResolve) {
          return rawType.getGenericInterfaces()[i];
        } else if (toResolve.isAssignableFrom(interfaces[i])) {
          return getGenericSupertype(rawType.getGenericInterfaces()[i], interfaces[i], toResolve);
        }
      }
    }

    // check our supertypes
    if (!rawType.isInterface()) {
      while (rawType != Object.class) {
        Class<?> rawSupertype = rawType.getSuperclass();
        if (rawSupertype == toResolve) {
          return rawType.getGenericSuperclass();
        } else if (toResolve.isAssignableFrom(rawSupertype)) {
          return getGenericSupertype(rawType.getGenericSuperclass(), rawSupertype, toResolve);
        }
        rawType = rawSupertype;
      }
    }

    // we can't resolve this further
    return toResolve;
  }

  public static String parseCharset(String mimeType) {
    Matcher match = CHARSET.matcher(mimeType);
    if (match.find()) {
      return match.group(1).replaceAll("[\"\\\\]", "");
    }
    return "UTF-8";
  }

  static class SynchronousExecutor implements Executor {
    @Override public void execute(Runnable runnable) {
      runnable.run();
    }
  }

  private Utils() {
    // No instances.
  }
}