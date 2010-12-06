// Copyright 2010 Square, Inc.
package retrofit.core;

/**
 * Information for a client error screen or dialog, including the screen title,
 * message, and button label. Any field may be null.
 *
 * @author Eric Burke (eric@squareup.com)
 */
public class ClientMessage {
  private final String title;
  private final String message;
  private final String buttonLabel;

  /**
   * Constructs a new message, any argument may be null.
   *
   * @param title       a few words for a dialog title or heading, or null.
   * @param message     a sentence or two with a more detailed, user friendly
   *                    message, or null.
   * @param buttonLabel text to display on a button, or null.
   */
  public ClientMessage(String title, String message, String buttonLabel) {
    this.title = title;
    this.message = message;
    this.buttonLabel = buttonLabel;
  }

  /** Returns a few words useful for a dialog title or heading, might be null. */
  public String getTitle() {
    return title;
  }

  /** Returns a sentence or two with a user friendly message. */
  public String getMessage() {
    return message;
  }

  /** Returns text for a button, or null. */
  public String getButtonLabel() {
    return buttonLabel;
  }
}
