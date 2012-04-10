// Copyright 2010 Square, Inc.
package retrofit.http;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import retrofit.core.Callback;
import retrofit.core.MainThread;
import retrofit.core.ProgressListener;
import retrofit.io.ByteSink;

import static retrofit.core.internal.Objects.nonNull;

/**
 * Fetches URL contents to files.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public class Fetcher {
  private static final Logger logger =
      Logger.getLogger(Fetcher.class.getName());

  // TODO: Support conditional get.

  private final Gson gson;
  private final Provider<HttpClient> httpClientProvider;
  private final Executor executor;
  private final MainThread mainThread;

  @Inject Fetcher(Gson gson, Provider<HttpClient> httpClientProvider, Executor executor, MainThread mainThread) {
    this.gson = gson;
    this.httpClientProvider = httpClientProvider;
    this.executor = executor;
    this.mainThread = mainThread;
  }

  /**
   * Fetches the contents of a URL to a file.
   *
   * @param url to fetch
   * @param destination for download
   * @param callback to invoke in the UI thread once the download completes
   * @param progressListener listens for progress, can be null
   */
  public void fetch(final String url, final ByteSink.Factory destination,
      Callback<Void> callback, final ProgressListener progressListener) {
    final HttpGet get = new HttpGet(nonNull(url, "url"));
    nonNull(destination, "destination");
    final UiCallback<Void> uiCallback
        = new UiCallback<Void>(nonNull(callback, "callback"), mainThread);
    executor.execute(new Runnable() {
      public void run() {
        try {
          httpClientProvider.get().execute(get, new DownloadHandler(gson, destination,
              uiCallback, progressListener, mainThread));
        } catch (IOException e) {
          logger.log(Level.WARNING, "fetch exception", e);
          uiCallback.networkError();
        } catch (Throwable t) {
          uiCallback.unexpectedError(t);
        }
      }
    });
  }

  static class DownloadHandler extends CallbackResponseHandler<Void> {

    /**
     * Throttles how often we send progress updates. Specified in %.
     */
    private static final int PROGRESS_GRANULARITY = 5;

    private final ByteSink.Factory destination;
    private final ProgressListener progressListener;
    private final MainThread mainThread;
    private final ProgressUpdate progressUpdate = new ProgressUpdate();

    DownloadHandler(Gson gson, ByteSink.Factory destination,
        UiCallback<Void> callback, ProgressListener progressListener,
        MainThread mainThread) {
      super(gson, callback);
      this.destination = destination;
      this.progressListener = progressListener;
      this.mainThread = mainThread;
    }

    @Override protected Void parse(HttpEntity entity) throws IOException {
      // Save the result to the sink instead of returning it.
      InputStream in = entity.getContent();
      try {
        ByteSink out = destination.newSink();
        final int total = (int) entity.getContentLength();
        int totalRead = 0;
        try {
          byte[] buffer = new byte[4096];
          int read;
          while ((read = in.read(buffer)) > -1) {
            out.write(buffer, read);
            if (progressListener != null) {
              totalRead += read;
              int percent = totalRead * 100 / total;
              if (percent - progressUpdate.percent > PROGRESS_GRANULARITY) {
                progressUpdate.percent = percent;
                mainThread.execute(progressUpdate);
              }
            }
          }
        } finally {
          out.close();
        }
      } finally {
        in.close();
      }
      return null;
    }

    /** Invokes ProgressListener in UI thread. */
    private class ProgressUpdate implements Runnable {
      private volatile int percent;
      public void run() {
        progressListener.hearProgress(percent);
      }
    }
  }
}
