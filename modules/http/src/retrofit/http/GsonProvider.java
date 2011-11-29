// Copyright 2011 Square, Inc.
package retrofit.http;

import retrofit.internal.gson.Gson;

/**
 * Creating a Gson instance is relatively expensive (70ms on my MBP), and Gson is thread-safe.
 * Create the instance once so we're not constantly creating them.
 *
 * @author Eric Denman (edenman@squareup.com)
 */
public class GsonProvider {
  /** Lazily loads the Gson instance. */
  private static class Holder {
    static final Gson gson = new Gson();
  }

  public static Gson gson() {
    return Holder.gson;
  }

}
