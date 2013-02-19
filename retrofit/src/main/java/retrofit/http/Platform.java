package retrofit.http;

import android.os.Process;
import com.google.gson.Gson;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import retrofit.http.android.AndroidApacheClient;
import retrofit.http.android.MainThreadExecutor;
import retrofit.http.client.ApacheClient;
import retrofit.http.client.Client;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static retrofit.http.RestAdapter.THREAD_PREFIX;
import static retrofit.http.Utils.SynchronousExecutor;

abstract class Platform {
  private static final Platform PLATFORM = findPlatform();

  static Platform get() {
    return PLATFORM;
  }

  private static Platform findPlatform() {
    try {
      Class.forName("android.os.Build");
      return new Android();
    } catch (ClassNotFoundException e) {
      return new Base();
    }
  }

  Converter defaultConverter() {
    return new GsonConverter(new Gson());
  }
  abstract Client.Provider defaultClient();
  abstract Executor defaultHttpExecutor();
  abstract Executor defaultCallbackExecutor();

  /** Provides sane defaults for operation on the JVM. */
  private static class Base extends Platform {
    @Override Client.Provider defaultClient() {
      final Client client = new ApacheClient();
      return new Client.Provider() {
        @Override public Client get() {
          return client;
        }
      };
    }

    @Override Executor defaultHttpExecutor() {
      return Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger threadCounter = new AtomicInteger();

        @Override public Thread newThread(final Runnable r) {
          return new Thread(new Runnable() {
            @Override public void run() {
              Thread.currentThread().setPriority(THREAD_PRIORITY_BACKGROUND);
              r.run();
            }
          }, THREAD_PREFIX + threadCounter.getAndIncrement());
        }
      });
    }

    @Override Executor defaultCallbackExecutor() {
      return new SynchronousExecutor();
    }
  }

  /** Provides sane defaults for operation on Android. */
  private static class Android extends Platform {
    @Override Client.Provider defaultClient() {
      final Client client = new AndroidApacheClient();
      return new Client.Provider() {
        @Override public Client get() {
          return client;
        }
      };
    }

    @Override Executor defaultHttpExecutor() {
      return Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger threadCounter = new AtomicInteger();

        @Override public Thread newThread(final Runnable r) {
          return new Thread(new Runnable() {
            @Override public void run() {
              Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);
              r.run();
            }
          }, THREAD_PREFIX + threadCounter.getAndIncrement());
        }
      });
    }

    @Override Executor defaultCallbackExecutor() {
      return new MainThreadExecutor();
    }
  }
}
