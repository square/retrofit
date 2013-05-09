// Copyright 2013 Square, Inc.
package retrofit.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Replaces the header with the the value of its target. If the target is null,
 * the header is removed.
 *
 * <p/>
 * ex.
 *
 * <pre>
 * @GET("/")
 * void foo(@Header("Auth-Token") String token, ..);
 * </pre>
 *
 * @author Adrian Cole (adrianc@netflix.com)
 */
@Retention(RUNTIME) @Target(PARAMETER)
public @interface Header {
  String value();
}
