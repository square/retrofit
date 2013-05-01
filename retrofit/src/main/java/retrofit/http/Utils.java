// Copyright 2012 Square, Inc.
// Copyright 2007 The Guava Authors
package retrofit.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import retrofit.http.client.Response;
import retrofit.http.mime.TypedByteArray;
import retrofit.http.mime.TypedInput;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public final class Utils {
  private static final Pattern CHARSET = Pattern.compile("\\Wcharset=([^\\s;]+)", CASE_INSENSITIVE);
  private static final int BUFFER_SIZE = 0x1000;

  /**
   * Creates a {@code byte[]} from reading the entirety of an {@link InputStream}. May return an
   * empty array but never {@code null}.
   * <p>
   * Copied from Guava's {@code ByteStreams} class.
   */
  static byte[] streamToBytes(InputStream stream) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    if (stream != null) {
      byte[] buf = new byte[BUFFER_SIZE];
      int r;
      while ((r = stream.read(buf)) != -1) {
        baos.write(buf, 0, r);
      }
    }
    return baos.toByteArray();
  }

  /**
   * Conditionally replace a {@link Response} with an identical copy whose body is backed by a
   * byte[] rather than an input stream.
   */
  static Response readBodyToBytesIfNecessary(Response response) throws IOException {
    TypedInput body = response.getBody();
    if (body == null || body instanceof TypedByteArray) {
      return response;
    }

    String bodyMime = body.mimeType();
    byte[] bodyBytes = Utils.streamToBytes(body.in());
    body = new TypedByteArray(bodyMime, bodyBytes);

    return replaceResponseBody(response, body);
  }

  static Response replaceResponseBody(Response response, TypedInput body) {
    return new Response(response.getStatus(), response.getReason(), response.getHeaders(), body);
  }

  public static String parseCharset(String mimeType) {
    Matcher match = CHARSET.matcher(mimeType);
    if (match.find()) {
      return match.group(1).replaceAll("[\"\\\\]", "");
    }
    return "UTF-8";
  }

  static class SynchronousExecutor implements Executor {
    @Override public void execute(Runnable runnable) {
      runnable.run();
    }
  }

  private Utils() {
    // No instances.
  }
}
