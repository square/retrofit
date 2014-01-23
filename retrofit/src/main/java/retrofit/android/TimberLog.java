package retrofit.android;

import timber.log.Timber;

/** A {@link retrofit.RestAdapter.Log logger} for Android that uses the Timber library. */
public class TimberLog extends AndroidLog {
  public TimberLog(String tag) {
    super(tag);
  }

  @Override public void logChunk(String chunk) {
    Timber.tag(getTag()).d(chunk);
  }
}
