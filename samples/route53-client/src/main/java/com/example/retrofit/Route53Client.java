/*
 * Copyright (C) 2012 Square, Inc.
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
package com.example.retrofit;

import com.squareup.okhttp.internal.Base64;
import org.xml.sax.helpers.DefaultHandler;
import retrofit.RequestHeaders;
import retrofit.RestAdapter;
import retrofit.client.Header;
import retrofit.converter.SAXConverter;
import retrofit.http.GET;
import retrofit.mime.TypedOutput;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Route53Client {
  private static final String API_URL = "https://route53.amazonaws.com";

  interface Route53 {
    @GET("/2012-02-29/hostedzone")
    List<HostedZone> hostedZones();
  }

  static class HostedZone {
    String id;
    String name;
  }

  public static void main(String... args) {
    // Create a REST adapter which points the Route53 API endpoint.
    RestAdapter restAdapter = new RestAdapter.Builder()
        .setServer(API_URL)
        .setConverter(new Route53Converter())
        .setRequestHeaders(new RestAuthentication(args[0], args[1]))
        .build();

    // Create an instance of our Route53 API interface.
    Route53 route53 = restAdapter.create(Route53.class);

    // Fetch and print a list of the domains you control and their ids.
    List<HostedZone> hostedZones = route53.hostedZones();
    for (HostedZone hostedZone : hostedZones) {
      System.out.println(hostedZone.name + " id:" + hostedZone.id);
    }
  }

  static class Route53Converter extends SAXConverter {
    @Override
    protected Deserializer newDeserializer(Type type) {
      return new ListHostedZonesResponseHandler();
    }

    @Override
    public TypedOutput toBody(Object object) {
      throw new UnsupportedOperationException();
    }
  }

  static class ListHostedZonesResponseHandler extends DefaultHandler
      implements SAXConverter.Deserializer {

    private StringBuilder currentText = new StringBuilder();
    private List<HostedZone> hostedZones = new ArrayList<HostedZone>();
    private HostedZone current = new HostedZone();

    @Override
    public List<HostedZone> getResult() {
      return hostedZones;
    }

    @Override
    public void endElement(String uri, String name, String qName) {
      if ("Id".equals(qName)) {
        current.id = currentText.toString().trim().replace("/hostedzone/", "");
      } else if ("Name".equals(qName)) {
        current.name = currentText.toString().trim();
      } else if ("HostedZone".equals(qName)) {
        hostedZones.add(current);
        current = new HostedZone();
      }
      currentText = new StringBuilder();
    }

    @Override
    public void characters(char[] ch, int start, int length) {
      currentText.append(ch, start, length);
    }
  }

  static class RestAuthentication implements RequestHeaders {

    private final String accessKey;
    private final String secretKey;

    public RestAuthentication(String accessKey, String secretKey) {
      this.accessKey = accessKey;
      this.secretKey = secretKey;
    }

    @Override
    public List<Header> get() {
      List<Header> headers = new ArrayList<Header>();
      String date = rfc1123DateFormat(new Date(System.currentTimeMillis()));
      String signature = sign(date);
      String auth = "AWS3-HTTPS AWSAccessKeyId=" + accessKey
          + ",Algorithm=HmacSHA256,Signature=" + signature;
      headers.add(new Header("Date", date));
      headers.add(new Header("X-Amzn-Authorization", auth));
      return headers;
    }

    String rfc1123DateFormat(Date date) {
      return new SimpleDateFormat("EEE, dd MMM yyyyy HH:mm:ss Z", Locale.US).format(date);
    }

    String sign(String toSign) {
      try {
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(keySpec);
        byte[] result = mac.doFinal(toSign.getBytes("UTF-8"));
        return Base64.encode(result);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
