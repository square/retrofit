// Copyright 2010 Square, Inc.
package retrofit.io.internal;

/**
 * Object utility methods.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public class Objects {

  /**
   * Returns t unless it's null.
   *
   * @throws NullPointerException if t is null
   */
  public static <T> T nonNull(T t, String name) {
    if (t == null) throw new NullPointerException(name);
    return t;
  }
}
