// Copyright 2013 Square, Inc.
package retrofit.mime;

import java.io.ByteArrayOutputStream;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static org.fest.assertions.api.Assertions.assertThat;

public class FormUrlEncodingTypedOutputTest {
  @Test public void urlEncoding() throws Exception {
    FormUrlEncodedTypedOutput fe = new FormUrlEncodedTypedOutput();
    fe.addField("a&b", "c=d");
    fe.addField("space, the", "final frontier");

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    fe.writeTo(out);
    String actual = new String(out.toByteArray(), "UTF-8");
    assertThat(actual).isEqualTo("a%26b=c%3Dd&space%2C+the=final+frontier");
  }

  @Test public void utf8encoding() throws Exception {
    FormUrlEncodedTypedOutput fe = new FormUrlEncodedTypedOutput();
    fe.addField("ooɟ", "ɹɐq");

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    fe.writeTo(out);
    String actual = new String(out.toByteArray(), "UTF-8");
    assertThat(actual).isEqualTo("oo%C9%9F=%C9%B9%C9%90q");
  }

  @Test public void encodedPairs() throws Exception {
    FormUrlEncodedTypedOutput fe = new FormUrlEncodedTypedOutput();
    fe.addField("sim", "ple");

    ByteArrayOutputStream out1 = new ByteArrayOutputStream();
    fe.writeTo(out1);
    String actual1 = new String(out1.toByteArray(), "UTF-8");
    assertThat(actual1).isEqualTo("sim=ple");

    fe.addField("hey", "there");
    fe.addField("help", "me");

    ByteArrayOutputStream out2 = new ByteArrayOutputStream();
    fe.writeTo(out2);
    String actual2 = new String(out2.toByteArray(), "UTF-8");
    assertThat(actual2).isEqualTo("sim=ple&hey=there&help=me");
  }

  @Test public void arrayParameter() throws Exception {
    FormUrlEncodedTypedOutput fe = new FormUrlEncodedTypedOutput();
    fe.addField("ping", new String[]{"pong", "pong-too"});

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    fe.writeTo(out);
    String actual = new String(out.toByteArray(), "UTF-8");
    assertThat(actual).isEqualTo("ping=pong&ping=pong-too");
  }

  @Test public void arrayPrimitiveParameter() throws Exception {
    FormUrlEncodedTypedOutput fe = new FormUrlEncodedTypedOutput();
    fe.addField("ping", new int[]{1, 2});

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    fe.writeTo(out);
    String actual = new String(out.toByteArray(), "UTF-8");
    assertThat(actual).isEqualTo("ping=1&ping=2");
  }

  @Test public void arrayIterable() throws Exception {
    FormUrlEncodedTypedOutput fe = new FormUrlEncodedTypedOutput();
    fe.addField("ping", Arrays.asList("pong", "pong-too"));

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    fe.writeTo(out);
    String actual = new String(out.toByteArray(), "UTF-8");
    assertThat(actual).isEqualTo("ping=pong&ping=pong-too");
  }
}
