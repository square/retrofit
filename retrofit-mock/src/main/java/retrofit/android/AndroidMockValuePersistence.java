// Copyright 2013 Square, Inc.
package retrofit.android;

import android.content.SharedPreferences;
import retrofit.MockRestAdapter;

/**
 * A {@link MockRestAdapter.ValueChangeListener value change listener} for {@link MockRestAdapter}
 * which stores any customized behavior values into shared preferences.
 */
public final class AndroidMockValuePersistence implements MockRestAdapter.ValueChangeListener {
  private static final String KEY_DELAY = "retrofit-mock-delay";
  private static final String KEY_VARIANCE = "retrofit-mock-variance";
  private static final String KEY_ERROR = "retrofit-mock-error";

  /**
   * Install a {@link MockRestAdapter.ValueChangeListener value change listener} on the supplied
   * {@link MockRestAdapter} using the {@link SharedPreferences} for storing customized behavior
   * values. Invoking this will load any existing stored values for the mock adapter's behavior.
   */
  public static void install(MockRestAdapter mockRestAdapter, SharedPreferences preferences) {
    long delay = preferences.getLong(KEY_DELAY, -1);
    if (delay != -1) {
      mockRestAdapter.setDelay(delay);
    }

    int variance = preferences.getInt(KEY_VARIANCE, -1);
    if (variance != -1) {
      mockRestAdapter.setVariancePercentage(variance);
    }

    int error = preferences.getInt(KEY_ERROR, -1);
    if (error != -1) {
      mockRestAdapter.setErrorPercentage(error);
    }

    mockRestAdapter.setValueChangeListener(new AndroidMockValuePersistence(preferences));
  }

  private final SharedPreferences preferences;

  private AndroidMockValuePersistence(SharedPreferences preferences) {
    this.preferences = preferences;
  }

  @Override public void onMockValuesChanged(long delayMs, int variancePct, int errorPct) {
    preferences.edit()
        .putLong(KEY_DELAY, delayMs)
        .putInt(KEY_VARIANCE, variancePct)
        .putInt(KEY_ERROR, errorPct)
        .apply();
  }
}
