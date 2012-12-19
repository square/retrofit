// Copyright 2012 Square, Inc.
package retrofit.http;

import java.lang.reflect.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static retrofit.http.RestAdapter.UTF_8;

final class Utils {
  private static final Pattern CHARSET = Pattern.compile("\\Wcharset=([^\\s;]+)", CASE_INSENSITIVE);

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

  static String parseCharset(String headerValue) {
    Matcher match = CHARSET.matcher(headerValue);
    if (match.find()) {
      return match.group(1).replaceAll("[\"\\\\]", "");
    }
    return UTF_8;
  }
}