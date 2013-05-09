// Copyright 2013 Square, Inc.
package retrofit.http;

import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Test;

public class HeaderPairTest {
  @Test(expected = NullPointerException.class) public void failOnNullName() {
    new HeaderPair(null, "value");
  }

  @Test(expected = NullPointerException.class) public void failOnNullValue() {
    new HeaderPair("name", null);
  }

  @Test public void mixedCaseNumericHyphenNameIsValid() {
    HeaderPair pair = new HeaderPair("Content-MD5", "iB94gawbwUSiZy5FuruIOQ==");
    assertThat(pair.getName()).isEqualTo("Content-MD5");
    assertThat(pair.getValue()).isEqualTo("iB94gawbwUSiZy5FuruIOQ==");
  }

  @Test(expected = IllegalArgumentException.class) public void failOnEmptyName() {
    new HeaderPair("", "value");
  }

  @Test(expected = IllegalArgumentException.class) public void failOnNameOutsideCharacterRange() {
    new HeaderPair("name_", "value");
  }

  @Test public void equalsHashCodeCaseInsensitiveOnName() {
    HeaderPair pair = new HeaderPair("Content-MD5", "iB94gawbwUSiZy5FuruIOQ==");
    HeaderPair lowerPair = new HeaderPair("content-md5", "iB94gawbwUSiZy5FuruIOQ==");
    assertThat(pair).isEqualTo(lowerPair);
    assertThat(pair.hashCode()).isEqualTo(lowerPair.hashCode());
  }

  @Test public void equalsHashCodeCaseSensitiveOnValue() {
    HeaderPair pair = new HeaderPair("Content-Type", "text/plain");
    HeaderPair lowerPair = new HeaderPair("Content-Type", "TEXT/PLAIN");
    assertThat(pair).isNotEqualTo(lowerPair);
    assertThat(pair.hashCode()).isNotEqualTo(lowerPair.hashCode());
  }
}
