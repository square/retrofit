// Copyright 2012 Square, Inc.
package retrofit.http;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for dealing with HttpClient.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public class HttpClients {
  private static final Logger LOGGER = Logger.getLogger(HttpClients.class.getName());

  /** Copies a response (so we can read it a second time) and logs it. */
  public static HttpEntity copyAndLog(HttpEntity entity, String url, Date start, DateFormat dateFormat)
      throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    entity.writeTo(bout);
    byte[] bytes = bout.toByteArray();
    // TODO: Use correct encoding.
    if (LOGGER.isLoggable(Level.FINE)) {
      final int chunkSize = 4000;
      long msElapsed = System.currentTimeMillis() - start.getTime();
      final String startTime = dateFormat.format(start);
      LOGGER.fine("----Response from " + url + " at " + startTime + " (" + msElapsed + "ms):");
      for (int i = 0; i < bytes.length; i += chunkSize) {
        int end = i + chunkSize;
        LOGGER.fine(((end > bytes.length) ? new String(bytes, i, bytes.length - i)
                                          : new String(bytes, i, chunkSize)));
      }
      LOGGER.fine("----end response.");
    }

    return new ByteArrayEntity(bytes);
  }
}
