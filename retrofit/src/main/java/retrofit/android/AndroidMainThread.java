// Copyright 2012 Square, Inc.
package retrofit.android;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import retrofit.http.MainThread;

import java.util.concurrent.CountDownLatch;

/** Executor that runs tasks on Android's main thread. */
public class AndroidMainThread implements MainThread {

  private final Looper looper = Looper.getMainLooper();
  private final Handler handler = new Handler(looper);

  @Override public void execute(Runnable r) {
    handler.post(r);
  }

  @Override public void executeDelayed(Runnable r, long delay) {
    handler.postAtTime(r, SystemClock.uptimeMillis() + delay);
  }

  @Override
  public void executeSynchronously(final Runnable runnable) {
    if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
      runnable.run();
    } else {
      final CountDownLatch latch = new CountDownLatch(1);
      execute(new Runnable() {
        @Override public void run() {
          try {
            runnable.run();
          } finally {
            latch.countDown();
          }
        }
      });
      while (true) {
        try {
          latch.await();
          return;
        } catch (InterruptedException e) { /* ignore */ }
      }
    }
  }

  @Override public void executeOnMain(Runnable runnable) {
    if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
      runnable.run();
    } else {
      execute(runnable);
    }
  }

  @Override public void cancel(Runnable r) {
    handler.removeCallbacks(r);
  }
}
