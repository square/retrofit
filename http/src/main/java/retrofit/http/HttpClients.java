// Copyright 2010 Square, Inc.
package retrofit.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
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
  private static final Logger LOGGER =
      Logger.getLogger(HttpClients.class.getName());

  /**
   * Converts an HttpEntity to a byte[].
   *
   * @throws NullPointerException if the entity is null
   */
  public static byte[] entityToBytes(HttpEntity entity) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    entity.writeTo(bout);
    return bout.toByteArray();
  }

  /**
   * Converts an HTTP response to an IOException.
   */
  public static IOException responseToException(HttpResponse response) {
    StatusLine statusLine = response.getStatusLine();
    String body = null;
    try {
      // TODO: Ensure entity is text-based and specify encoding.
      HttpEntity entity = response.getEntity();
      if (entity != null) body = new String(entityToBytes(entity));
    } catch (Throwable t) {
      // The original error takes precedence.
      LOGGER.log(Level.WARNING, "Response entity to String conversion.", t);
    }
    return new IOException("Unexpected response."
        + " Code: " + statusLine.getStatusCode()
        + ", Reason: " + statusLine.getReasonPhrase()
        + ", Body: " + body);
  }

  /**
   * Copies a response (so we can read it a second time) and logs it.
   */
  public static HttpEntity copyAndLog(HttpEntity entity, String url, Date start, DateFormat dateFormat)
      throws IOException {
    byte[] bytes = entityToBytes(entity);
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
