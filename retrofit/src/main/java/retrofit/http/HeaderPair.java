// Copyright 2013 Square, Inc.
package retrofit.http;
import static java.lang.String.format;
import java.util.regex.Pattern;

/** Represents an HTTP header name/value pair. */
public final class HeaderPair {
  // Matches alpha-numeric, mixed-case, or hyphens, not leading with a hyphen.
  private static final Pattern HEADER_NAME = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9-]*$");

  private final String name;
  private final String value;

  public HeaderPair(String name, String value) {
    this.name = name;
    if (name == null)
      throw new NullPointerException("header name was null");
    if (!HEADER_NAME.matcher(name).find())
      throw new IllegalArgumentException(format(
          "header %s doesn't match pattern: %s", name, HEADER_NAME));
    this.value = value;
    if (value == null)
      throw new NullPointerException("header value was null for: " + name);
  }

  /**
   * Note: per RFC 2616: names are case-insensitive.
   */
  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HeaderPair header = (HeaderPair) o;

    // RFC 2616: Field names are case-insensitive
    if (!name.equalsIgnoreCase(header.name)) return false;
    if (!value.equals(header.value)) return false;

    return true;
  }

  @Override public int hashCode() {
    // RFC 2616: Field names are case-insensitive
    int result = name.toLowerCase().hashCode();
    result = 31 * result + value.hashCode();
    return result;
  }

  @Override public String toString() {
    return (name != null ? name : "") + ": " + (value != null ? value : "");
  }
}
