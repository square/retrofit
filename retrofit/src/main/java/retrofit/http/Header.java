// Copyright 2012 Square, Inc.
package retrofit.http;

/** Represents an HTTP header name/value pair. */
public final class Header {
  private final String name;
  private final String value;

  public Header(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Header header = (Header) o;

    if (name != null ? !name.equals(header.name) : header.name != null) return false;
    if (value != null ? !value.equals(header.value) : header.value != null) return false;

    return true;
  }

  @Override public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }

  @Override public String toString() {
    return (name != null ? name : "") + ": " + (value != null ? value : "");
  }
}
