package retrofit.util;

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

  /** Returns true if the two possibly objects are equal. */
  public static <T> boolean equal(T a, T b) {
    return a == b || a != null && a.equals(b);
  }
}
