package retrofit.http;

import android.net.http.AndroidHttpClient;
import android.os.Process;
import com.google.gson.Gson;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Provider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import retrofit.android.MainThreadExecutor;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static retrofit.http.RestAdapter.SynchronousExecutor;
import static retrofit.http.RestAdapter.THREAD_PREFIX;

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
  abstract Provider<HttpClient> defaultHttpClient();
  abstract Executor defaultHttpExecutor();
  abstract Executor defaultCallbackExecutor();

  /** Provides sane defaults for operation on the JVM. */
  private static class Base extends Platform {
    @Override Provider<HttpClient> defaultHttpClient() {
      final HttpClient client = new DefaultHttpClient();
      return new Provider<HttpClient>() {
        @Override public HttpClient get() {
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
    @Override Provider<HttpClient> defaultHttpClient() {
      // TODO use HttpUrlConnection on Android 2.3+
      final HttpClient client = AndroidHttpClient.newInstance("Retrofit");
      return new Provider<HttpClient>() {
        @Override public HttpClient get() {
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
