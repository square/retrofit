// Copyright 2014 Square, Inc.
package retrofit;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EndpointsTest {
  @Test public void endpoint() {
    Endpoint endpoint = Endpoints.newFixedEndpoint("http://example.com");
    assertThat(endpoint.getUrl()).isEqualTo("http://example.com");
  }
}
