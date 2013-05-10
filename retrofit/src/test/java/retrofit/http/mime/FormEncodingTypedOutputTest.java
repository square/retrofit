// Copyright 2013 Square, Inc.
package retrofit.http.mime;

import java.io.ByteArrayOutputStream;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class FormEncodingTypedOutputTest {
  @Test public void urlEncoding() throws Exception {
    FormEncodedTypedOutput fe = new FormEncodedTypedOutput();
    fe.addPair("a&b", "c=d");
    fe.addPair("space, the", "final frontier");

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    fe.writeTo(out);
    String actual = new String(out.toByteArray(), "UTF-8");
    assertThat(actual).isEqualTo("a%26b=c%3Dd&space%2C+the=final+frontier");
  }

  @Test public void encodedPairs() throws Exception {
    FormEncodedTypedOutput fe = new FormEncodedTypedOutput();
    fe.addPair("sim", "ple");

    ByteArrayOutputStream out1 = new ByteArrayOutputStream();
    fe.writeTo(out1);
    String actual1 = new String(out1.toByteArray(), "UTF-8");
    assertThat(actual1).isEqualTo("sim=ple");

    fe.addPair("hey", "there");
    fe.addPair("help", "me");

    ByteArrayOutputStream out2 = new ByteArrayOutputStream();
    fe.writeTo(out2);
    String actual2 = new String(out2.toByteArray(), "UTF-8");
    assertThat(actual2).isEqualTo("sim=ple&hey=there&help=me");
  }
}
