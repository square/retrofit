// Copyright 2014 Square, Inc.
package retrofit;

import com.squareup.okhttp.HttpUrl;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class EndpointsTest {
  @Test public void endpoint() {
    Endpoint endpoint = Endpoint.createFixed("http://example.com");
    assertThat(endpoint.url()).isEqualTo(HttpUrl.parse("http://example.com"));
  }

  @Test public void invalidEndpointEagerlyThrows() {
    try {
      Endpoint.createFixed("ftp://foo");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Invalid URL: ftp://foo");
    }
  }

  @Test public void nullUrlThrows() {
    try {
      Endpoint.createFixed(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("url == null");
    }
  }

  @Test public void emptyUrlThrows() {
    try {
      Endpoint.createFixed(" ");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Empty URL");
    }
  }
}
