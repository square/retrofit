package retrofit;

import com.google.gson.Gson;
import org.junit.Test;
import retrofit.client.Header;
import retrofit.client.Response;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedString;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class RetrofitErrorTest {

  @Test
  public void everyRetrofitErrorHasAMessage() throws Exception {
    String url = "http://example.com/";
    Response response = new Response(200, "OK", new ArrayList<Header>(),
        new TypedString("\"foo\""));
    Converter conv = new GsonConverter(new Gson());

    assertThat(RetrofitError.networkError(url, new IOException()).getMessage())
        .isNotNull()
        .isNotEmpty()
        .contains(url);

    assertThat(RetrofitError.conversionError(url, response, conv, String.class, null).getMessage())
        .isNotNull()
        .isNotEmpty()
        .contains(url)
        .contains("200 OK");

    assertThat(RetrofitError.httpError(url, response, conv, String.class).getMessage())
        .isNotNull()
        .isNotEmpty()
        .contains(url)
        .contains("200 OK");

    assertThat(RetrofitError.unexpectedError(url, new IOException()).getMessage())
        .isNotNull()
        .isNotEmpty()
        .contains(url);
  }

  @Test
  public void isNetworkErrorIsOnlyTrueForNetworkErrors() throws Exception {
    assertThat(RetrofitError.networkError(null, null).isNetworkError()).isTrue();
    assertThat(RetrofitError.conversionError(null, null, null, null, null).isNetworkError())
        .isFalse();
    assertThat(RetrofitError.httpError(null, null, null, null).isNetworkError()).isFalse();
    assertThat(RetrofitError.unexpectedError(null, null).isNetworkError()).isFalse();
  }

  @Test
  public void getBodyReturnsNullIfResponseIsNull() throws Exception {
    assertThat(RetrofitError.networkError(null, null).getBody()).isNull();
    assertThat(RetrofitError.conversionError(null, null, null, null, null).getBody()).isNull();
    assertThat(RetrofitError.httpError(null, null, null, null).getBody()).isNull();
    assertThat(RetrofitError.unexpectedError(null, null).getBody()).isNull();
  }

  @Test
  public void getBodyReturnsNullIfResponseBodyIsNull() throws Exception {
    Response dummyResponse = new Response(204, "No Content", new ArrayList<Header>(), null);
    assertThat(RetrofitError.conversionError(null, dummyResponse, null, null, null).getBody())
        .isNull();
    assertThat(RetrofitError.httpError(null, dummyResponse, null, null).getBody()).isNull();
  }

  @Test
  public void getBodyThrowsRuntimeExceptionIfBodyIsPresentButConverterIsNot() throws Exception {
    Response dummyResponse = new Response(200, "OK", new ArrayList<Header>(),
        new TypedString("\"foo\""));

    try {
      RetrofitError.conversionError(null, dummyResponse, null, null, null).getBody();
      fail("A RuntimeException should have been thrown.");
    } catch (RuntimeException e) {
      // ok
    }

    try {
      RetrofitError.httpError(null, dummyResponse, null, null).getBody();
      fail("A RuntimeException should have been thrown.");
    } catch (RuntimeException e) {
      // ok
    }
  }

  @Test
  public void getBodyReturnsConvertedBody() throws Exception {
    Response dummyResponse = new Response(200, "OK", new ArrayList<Header>(),
        new TypedString("\"foo\""));
    Converter converter = new GsonConverter(new Gson());

    assertThat((String) RetrofitError.conversionError(null, dummyResponse, converter,
        String.class, null).getBody())
        .isEqualTo("foo");

    assertThat((String) RetrofitError.httpError(null, dummyResponse, converter, String.class)
        .getBody()).isEqualTo("foo");
  }

  @Test
  public void getBodyThrowsRuntimeExceptionIfConversionExceptionWasThrown() throws Exception {
    Response dummyResponse = new Response(200, "OK", new ArrayList<Header>(),
        new TypedString("\"foo\""));

    Converter converter = mock(Converter.class);
    when(converter.fromBody(any(TypedInput.class), any(Type.class)))
        .thenThrow(new ConversionException("Expected."));

    try {
      RetrofitError.conversionError(null, dummyResponse, converter, String.class, null).getBody();
      fail("A RuntimeException should have been thrown.");
    } catch (RuntimeException e) {
      // ok
    }

    try {
      RetrofitError.httpError(null, dummyResponse, converter, String.class).getBody();
      fail("A RuntimeException should have been thrown.");
    } catch (RuntimeException e) {
      // ok
    }
  }

  @Test
  public void getBodyAsReturnsNullIfResponseIsNull() throws Exception {
    assertThat(RetrofitError.networkError(null, null).getBodyAs(String.class)).isNull();
    assertThat(RetrofitError.conversionError(null, null, null, null, null).getBodyAs(String.class))
        .isNull();
    assertThat(RetrofitError.httpError(null, null, null, null).getBodyAs(String.class)).isNull();
    assertThat(RetrofitError.unexpectedError(null, null).getBodyAs(String.class)).isNull();
  }

  @Test
  public void getBodyAsReturnsNullIfResponseBodyIsNull() throws Exception {
    Response dummyResponse = new Response(204, "No Content", new ArrayList<Header>(), null);
    assertThat(RetrofitError.conversionError(null, dummyResponse, null, null, null)
        .getBodyAs(String.class)).isNull();
    assertThat(RetrofitError.httpError(null, dummyResponse, null, null).getBodyAs(String.class)).isNull();
  }

  @Test
  public void getBodyAsThrowsRuntimeExceptionIfBodyIsPresentButConverterIsNot() throws Exception {
    Response dummyResponse = new Response(204, "No Content", new ArrayList<Header>(),
        new TypedString("\"foo\""));

    try {
      RetrofitError.conversionError(null, dummyResponse, null, null, null).getBodyAs(String.class);
      fail("A RuntimeException should have been thrown.");
    } catch (RuntimeException e) {
      // ok
    }

    try {
      RetrofitError.httpError(null, dummyResponse, null, null).getBodyAs(String.class);
      fail("A RuntimeException should have been thrown.");
    } catch (RuntimeException e) {
      // ok
    }
  }

  @Test
  public void getBodyAsReturnsConvertedBody() throws Exception {
    Response response = new Response(200, "OK", new ArrayList<Header>(),
        new TypedString("\"foo\""));
    Converter converter = new GsonConverter(new Gson());

    assertThat((String) RetrofitError.conversionError(null, response, converter, String.class, null)
        .getBodyAs(String.class)).isEqualTo("foo");

    assertThat((String) RetrofitError.httpError(null, response, converter, String.class)
        .getBodyAs(String.class)).isEqualTo("foo");
  }

  @Test
  public void getBodyAsThrowsRuntimeExceptionIfConversionExceptionWasThrown() throws Exception {
    Response dummyResponse = new Response(200, "OK", new ArrayList<Header>(),
        new TypedString("\"foo\""));

    Converter converter = mock(Converter.class);
    when(converter.fromBody(any(TypedInput.class), any(Type.class)))
        .thenThrow(new ConversionException("Expected."));

    try {
      RetrofitError.conversionError(null, dummyResponse, converter, String.class, null)
          .getBodyAs(String.class);
      fail("A RuntimeException should have been thrown.");
    } catch (RuntimeException e) {
      // ok
    }

    try {
      RetrofitError.httpError(null, dummyResponse, converter, String.class).getBodyAs(String.class);
      fail("A RuntimeException should have been thrown.");
    } catch (RuntimeException e) {
      // ok
    }
  }
}
