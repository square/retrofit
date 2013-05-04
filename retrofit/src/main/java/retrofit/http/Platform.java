package retrofit.http;

import android.os.Build;
import android.os.Process;
import android.util.Log;
import com.google.gson.Gson;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import retrofit.http.android.AndroidApacheClient;
import retrofit.http.android.MainThreadExecutor;
import retrofit.http.client.Client;
import retrofit.http.client.UrlConnectionClient;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static java.lang.Thread.MIN_PRIORITY;
import static retrofit.http.RestAdapter.IDLE_THREAD_NAME;
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
  abstract RestAdapter.Log defaultLog();

  /** Provides sane defaults for operation on the JVM. */
  private static class Base extends Platform {
    @Override Client.Provider defaultClient() {
      final Client client = new UrlConnectionClient();
      return new Client.Provider() {
        @Override public Client get() {
          return client;
        }
      };
    }

    @Override Executor defaultHttpExecutor() {
      return Executors.newCachedThreadPool(new ThreadFactory() {
        @Override public Thread newThread(final Runnable r) {
          return new Thread(new Runnable() {
            @Override public void run() {
              Thread.currentThread().setPriority(MIN_PRIORITY);
              r.run();
            }
          }, IDLE_THREAD_NAME);
        }
      });
    }

    @Override Executor defaultCallbackExecutor() {
      return new SynchronousExecutor();
    }

    @Override RestAdapter.Log defaultLog() {
      return new RestAdapter.Log() {
        @Override public void log(String message) {
          System.out.println(message);
        }
      };
    }
  }

  /** Provides sane defaults for operation on Android. */
  private static class Android extends Platform {
    @Override Client.Provider defaultClient() {
      final Client client;
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
        client = new AndroidApacheClient();
      } else {
        client = new UrlConnectionClient();
      }
      return new Client.Provider() {
        @Override public Client get() {
          return client;
        }
      };
    }

    @Override Executor defaultHttpExecutor() {
      return Executors.newCachedThreadPool(new ThreadFactory() {
        @Override public Thread newThread(final Runnable r) {
          return new Thread(new Runnable() {
            @Override public void run() {
              Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);
              r.run();
            }
          }, IDLE_THREAD_NAME);
        }
      });
    }

    @Override Executor defaultCallbackExecutor() {
      return new MainThreadExecutor();
    }

    @Override RestAdapter.Log defaultLog() {
      return new RestAdapter.Log() {
        @Override public void log(String message) {
          Log.d("Retrofit", message);
        }
      };
    }
  }
}
