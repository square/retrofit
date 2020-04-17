/*
 * Copyright (C) 2018 Square, Inc.
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
package retrofit2.converter.jaxb;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import javax.xml.bind.JAXBContext;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public final class JaxbConverterFactoryTest {
  static final Contact SAMPLE_CONTACT =
      new Contact("Jenny", Collections.singletonList(new PhoneNumber("867-5309", Type.MOBILE)));

  static final String SAMPLE_CONTACT_XML =
      ""
          + "<?xml version=\"1.0\" ?>"
          + "<contact>"
          + "<name>Jenny</name>"
          + "<phone_number type=\"MOBILE\">"
          + "<number>867-5309</number>"
          + "</phone_number>"
          + "</contact>";

  interface Service {
    @POST("/")
    Call<Void> postXml(@Body Contact contact);

    @GET("/")
    Call<Contact> getXml();
  }

  @Rule public final MockWebServer server = new MockWebServer();

  private Service service;

  @Before
  public void setUp() {
    JaxbConverterFactory factory = JaxbConverterFactory.create();
    Retrofit retrofit =
        new Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(factory).build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void xmlRequestBody() throws Exception {
    server.enqueue(new MockResponse());

    Call<Void> call = service.postXml(SAMPLE_CONTACT);
    call.execute();

    RecordedRequest request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/xml; charset=utf-8");
    assertThat(request.getBody().readUtf8()).isEqualTo(SAMPLE_CONTACT_XML);
  }

  @Test
  public void xmlResponseBody() throws Exception {
    server.enqueue(new MockResponse().setBody(SAMPLE_CONTACT_XML));

    Call<Contact> call = service.getXml();
    Response<Contact> response = call.execute();
    assertThat(response.body()).isEqualTo(SAMPLE_CONTACT);
  }

  @Test
  public void characterEncoding() throws Exception {
    server.enqueue(
        new MockResponse()
            .setBody(
                ""
                    + "<?xml version=\"1.0\" ?>"
                    + "<contact>"
                    + "<name>Бронтозавр \uD83E\uDD95 ティラノサウルス・レックス &#129430;</name>"
                    + "</contact>"));

    Call<Contact> call = service.getXml();
    Response<Contact> response = call.execute();
    assertThat(response.body().name)
        .isEqualTo("Бронтозавр \uD83E\uDD95 ティラノサウルス・レックス \uD83E\uDD96");
  }

  @Test
  public void userSuppliedJaxbContext() throws Exception {
    JAXBContext context = JAXBContext.newInstance(Contact.class);
    JaxbConverterFactory factory = JaxbConverterFactory.create(context);
    Retrofit retrofit =
        new Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(factory).build();
    service = retrofit.create(Service.class);

    server.enqueue(new MockResponse());

    Call<Void> call = service.postXml(SAMPLE_CONTACT);
    call.execute();

    RecordedRequest request = server.takeRequest();
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/xml; charset=utf-8");
    assertThat(request.getBody().readUtf8()).isEqualTo(SAMPLE_CONTACT_XML);
  }

  @Test
  public void malformedXml() throws Exception {
    server.enqueue(new MockResponse().setBody("This is not XML"));

    Call<Contact> call = service.getXml();
    try {
      call.execute();
      fail();
    } catch (RuntimeException expected) {
      assertThat(expected).hasMessageContaining("ParseError");
    }
  }

  @Test
  public void unrecognizedField() throws Exception {
    server.enqueue(
        new MockResponse()
            .setBody(
                ""
                    + "<?xml version=\"1.0\" ?>"
                    + "<contact>"
                    + "<name>Jenny</name>"
                    + "<age>21</age>"
                    + "<phone_number type=\"FAX\">"
                    + "<number>867-5309</number>"
                    + "</phone_number>"
                    + "</contact>"));

    Call<Contact> call = service.getXml();
    Response<Contact> response = call.execute();
    assertThat(response.body().name).isEqualTo("Jenny");
  }

  @Test
  public void externalEntity() throws Exception {
    server.enqueue(
        new MockResponse()
            .setBody(
                ""
                    + "<?xml version=\"1.0\" ?>"
                    + "<!DOCTYPE contact["
                    + "  <!ENTITY secret SYSTEM \""
                    + server.url("/secret.txt")
                    + "\">"
                    + "]>"
                    + "<contact>"
                    + "<name>&secret;</name>"
                    + "</contact>"));
    server.enqueue(new MockResponse().setBody("hello"));

    Call<Contact> call = service.getXml();
    try {
      Response<Contact> response = call.execute();
      response.body();
      fail();
    } catch (RuntimeException expected) {
      assertThat(expected).hasMessageContaining("ParseError");
    }

    assertThat(server.getRequestCount()).isEqualTo(1);
  }

  @Test
  public void externalDtd() throws Exception {
    server.enqueue(
        new MockResponse()
            .setBody(
                ""
                    + "<?xml version=\"1.0\" ?>"
                    + "<!DOCTYPE contact SYSTEM \""
                    + server.url("/contact.dtd")
                    + "\">"
                    + "<contact>"
                    + "<name>&secret;</name>"
                    + "</contact>"));
    server.enqueue(
        new MockResponse()
            .setBody(
                ""
                    + "<!ELEMENT contact (name)>\n"
                    + "<!ELEMENT name (#PCDATA)>\n"
                    + "<!ENTITY secret \"hello\">"));

    Call<Contact> call = service.getXml();
    try {
      Response<Contact> response = call.execute();
      response.body();
      fail();
    } catch (RuntimeException expected) {
      assertThat(expected).hasMessageContaining("ParseError");
    }

    assertThat(server.getRequestCount()).isEqualTo(1);
  }
}
