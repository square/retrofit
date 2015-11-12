package retrofit;

import org.junit.Test;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class RequestFactoryTest {

  private interface Service {
    void callWithoutParams();
    void callWithOneParam(Integer number);
    void callWithTwoParams(String param1, Object param2);
  }

  @Test public void toStringForMethodWithoutParams() throws NoSuchMethodException {
    RequestFactory requestFactory = new RequestFactory(Service.class.getMethod("callWithoutParams"),
        "GET", null, "/whereever1/", null, null, false, false, false, null);
    assertThat(requestFactory.toString())
    .isEqualTo("Service.callWithoutParams(), HTTP method = GET, relative path template = /whereever1/");
  }

  @Test public void toStringForMethodWithOneParam() throws NoSuchMethodException {
    RequestFactory requestFactory = new RequestFactory(
        Service.class.getMethod("callWithOneParam", Integer.class),
        "POST", null, "/whereever2/", null, null, false, false, false, null);
    assertThat(requestFactory.toString())
        .isEqualTo("Service.callWithOneParam(Integer), HTTP method = POST, relative path template = /whereever2/");
  }

  @Test public void toStringForMethodWithTwoParams() throws NoSuchMethodException {
    RequestFactory requestFactory = new RequestFactory(
        Service.class.getMethod("callWithTwoParams", String.class, Object.class),
        "DELETE", null, "/whereever3/", null, null, false, false, false, null);
    assertThat(requestFactory.toString())
        .isEqualTo("Service.callWithTwoParams(String,Object), HTTP method = DELETE, relative path template = /whereever3/");
  }
}