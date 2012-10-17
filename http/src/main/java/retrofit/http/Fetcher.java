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

import static retrofit.http.RestAdapter.DATE_FORMAT;

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
  public void fetch(final String url, final ByteSink.Factory destination, Callback<Void> callback,
        final ProgressListener progressListener) {
    if (url == null) throw new NullPointerException("url");
    if (destination == null) throw new NullPointerException("destination");
    if (callback == null) throw new NullPointerException("callback");
    final HttpGet get = new HttpGet(url);
    final UiCallback<Void> uiCallback = new UiCallback<Void>(callback, mainThread);
    executor.execute(new Runnable() {
      public void run() {
        try {
          DownloadHandler downloadHandler =
              new DownloadHandler(url, destination, uiCallback, progressListener, mainThread, new Date(), DATE_FORMAT);
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

  static class DownloadHandler extends CallbackResponseHandler<Void> {
    private final ByteSink.Factory destination;
    private final ProgressListener progressListener;
    private final MainThread mainThread;
    private final ProgressUpdate progressUpdate = new ProgressUpdate();

    DownloadHandler(String url, ByteSink.Factory destination, UiCallback<Void> callback,
          ProgressListener progressListener, MainThread mainThread, Date start, ThreadLocal<DateFormat> dateFormat) {
      super(callback, null, null, url, start, dateFormat);
      this.destination = destination;
      this.progressListener = progressListener;
      this.mainThread = mainThread;
    }

    @Override protected Object parse(HttpEntity entity, Type type) throws ConversionException {
      try {
        // Save the result to the sink instead of returning it.
        InputStream in = entity.getContent();
        try {
          ByteSink out = destination.newSink();
          final long total = entity.getContentLength();
          long totalRead = 0;
          try {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
              out.write(buffer, read);
              if (progressListener != null) {
                totalRead += read;
                progressUpdate.percent = 1f * totalRead / total;
                synchronized (progressUpdate) {
                  if (progressUpdate.run) {
                    progressUpdate.run = false;
                    mainThread.execute(progressUpdate);
                  }
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
      /** Amount to report when run. */
      volatile float percent;

      /** Whether or not this update has been run. Synchronized on 'this'. */
      boolean run = true;

      public void run() {
        progressListener.hearProgress(percent);
        synchronized (this) {
          run = true;
        }
      }
    }
  }
}
