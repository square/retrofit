// Copyright 2012 Square, Inc.
package retrofit.http;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import retrofit.io.ByteSink;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fetches URL contents to files.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public class Fetcher {
  private static final Logger LOGGER = Logger.getLogger(Fetcher.class.getName());

  // TODO: Support conditional get.

  private final Provider<HttpClient> httpClientProvider;
  private final Executor executor;
  private final MainThread mainThread;
  private final ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
    @Override protected DateFormat initialValue() {
      return super.initialValue();
    }
  };

  @Inject Fetcher(Provider<HttpClient> httpClientProvider, Executor executor, MainThread mainThread) {
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
  public void fetch(final String url, final ByteSink.Factory destination, Callback<Void, Void, Void> callback,
      final ProgressListener progressListener) {
    if (url == null) throw new NullPointerException("url");
    if (destination == null) throw new NullPointerException("destination");
    if (callback == null) throw new NullPointerException("callback");
    final HttpGet get = new HttpGet(url);
    final UiCallback<Void, Void, Void> uiCallback = new UiCallback<Void, Void, Void>(callback, mainThread);
    executor.execute(new Runnable() {
      public void run() {
        try {
          Date start = new Date();
          DownloadHandler downloadHandler =
              new DownloadHandler(url, destination, uiCallback, progressListener, mainThread, start, dateFormat);
          httpClientProvider.get().execute(get, downloadHandler);
        } catch (IOException e) {
          LOGGER.log(Level.WARNING, "fetch exception", e);
          uiCallback.networkError();
        } catch (Throwable t) {
          uiCallback.unexpectedError(t);
        }
      }
    });
  }

  static class DownloadHandler extends CallbackResponseHandler<Void, Void, Void> {
    /** Throttles how often we send progress updates. Specified in %. */
    private static final int PROGRESS_GRANULARITY = 5;

    private final ByteSink.Factory destination;
    private final ProgressListener progressListener;
    private final MainThread mainThread;
    private final ProgressUpdate progressUpdate = new ProgressUpdate();

    DownloadHandler(String url, ByteSink.Factory destination, UiCallback<Void, Void, Void> callback,
        ProgressListener progressListener, MainThread mainThread, Date start, ThreadLocal<DateFormat> dateFormat) {
      super(callback, new Type[3], null, url, start, dateFormat);
      this.destination = destination;
      this.progressListener = progressListener;
      this.mainThread = mainThread;
    }

    @Override protected <T> T parse(HttpEntity entity, Type type) throws ConversionException {
      try {
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
      } catch (IOException e) {
        throw new ConversionException(e);
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
