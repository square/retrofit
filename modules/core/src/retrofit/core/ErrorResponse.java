// Copyright 2010 Square, Inc.
package retrofit.core;

/**
 * Information for a client error screen or dialog, including the screen title
 * and message. Either field may be null.
 *
 * @author Eric Burke (eric@squareup.com)
 */
public class ErrorResponse {
  private final String title;
  private final String message;

  /**
   * Constructs a new message, though arguments may be null.
   *
   * @param title   a few words for a dialog title or heading, or null.
   * @param message a sentence or two with a more detailed, user friendly
   *                message, or null.
   */
  public ErrorResponse(String title, String message) {
    this.title = title;
    this.message = message;
  }

  /** Returns a few words useful for a dialog title or heading, might be null. */
  public String getTitle() {
    return title;
  }

  /** Returns a sentence or two with a user friendly message. */
  public String getMessage() {
    return message;
  }

  @Override public String toString() {
    return getClass().getSimpleName() + "["
        + "title = " + title
        + ", message = " + message + "]";
  }
}
