// Copyright 2012 Square, Inc.
package retrofit.android;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

/** Executor that runs tasks on Android's main thread. */
public class MainThreadExecutor implements Executor {
  private final Handler handler = new Handler(Looper.getMainLooper());

  @Override public void execute(Runnable r) {
    handler.post(r);
  }
}
