/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit.mime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public final class MimeUtil {
  private static final Pattern CHARSET = Pattern.compile("\\Wcharset=([^\\s;]+)", CASE_INSENSITIVE);

  /**
   * Parse the MIME type from a {@code Content-Type} header value or default to "UTF-8".
   *
   * @deprecated Use {@link #parseCharset(String, String)}.
   */
  @Deprecated
  public static String parseCharset(String mimeType) {
    return parseCharset(mimeType, "UTF-8");
  }

  /** Parse the MIME type from a {@code Content-Type} header value. */
  public static String parseCharset(String mimeType, String defaultCharset) {
    Matcher match = CHARSET.matcher(mimeType);
    if (match.find()) {
      return match.group(1).replaceAll("[\"\\\\]", "");
    }
    return defaultCharset;
  }

  private MimeUtil() {
    // No instances.
  }
}
