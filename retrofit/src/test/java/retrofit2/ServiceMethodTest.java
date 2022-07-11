package retrofit2;

import org.junit.Assert;
import org.junit.Test;
import retrofit2.http.GET;

import java.lang.reflect.Method;
import java.util.Arrays;

public class ServiceMethodTest {

    @Test
    public void testParseAnnotations() {
        // Setup
      //Can't set an adapter to test, so it should return an exception
        Retrofit retrofit = new Retrofit.Builder()
          .baseUrl("http://www.example.com")
          .build();

        Method method = Arrays.stream(ITest.class.getMethods()).findFirst().get();
      // Run the test
      Assert.assertThrows(IllegalArgumentException.class, () -> {
        ServiceMethod.parseAnnotations(retrofit, method);
      });
    }
}
interface ITest {
  @GET("/")
  Call<String> testGet();
}

class TestDTO {
  private String message;

  public TestDTO(String message) {
    this.message = message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return this.message;
  }
}
