// Copyright 2012 Square, Inc.
package retrofit.http;

import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;
import static retrofit.http.RestAdapter.UTF_8;
import static retrofit.http.Utils.parseCharset;

public class UtilsTest {
  @Test public void testCharsetParsing() {
    assertThat(parseCharset("text/plain;charset=utf-8")).isEqualToIgnoringCase(UTF_8);
    assertThat(parseCharset("text/plain; charset=utf-8")).isEqualToIgnoringCase(UTF_8);
    assertThat(parseCharset("text/plain;  charset=utf-8")).isEqualToIgnoringCase(UTF_8);
    assertThat(parseCharset("text/plain; \tcharset=utf-8")).isEqualToIgnoringCase(UTF_8);
    assertThat(parseCharset("text/plain; \r\n\tcharset=utf-8")).isEqualToIgnoringCase(UTF_8);
    assertThat(parseCharset("text/plain; CHARSET=utf-8")).isEqualToIgnoringCase(UTF_8);
    assertThat(parseCharset("text/plain; charset=UTF-8")).isEqualToIgnoringCase(UTF_8);
    assertThat(parseCharset("text/plain; charset=\"\\u\\tf-\\8\"")).isEqualToIgnoringCase(UTF_8);
    assertThat(parseCharset("text/plain; charset=\"utf-8\"")).isEqualToIgnoringCase(UTF_8);
    assertThat(parseCharset("text/plain; charset=utf-8; other=thing")).isEqualToIgnoringCase(UTF_8);
    assertThat(parseCharset("text/plain; notthecharset=utf-16;")).isEqualToIgnoringCase(UTF_8);
  }
}
