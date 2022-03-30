package retrofit2;

import kotlin.Unit;
import okhttp3.Request;
import org.junit.Test;
import retrofit2.http.HEAD;

import static org.assertj.core.api.Assertions.assertThat;
import static retrofit2.TestingUtils.buildRequest;

public final class KotlinRequestFactoryTest {
  @Test
  public void headUnit() {
    class Example {
      @HEAD("/foo/bar/")
      Call<Unit> method() {
        return null;
      }
    }

    Request request = buildRequest(Example.class);
    assertThat(request.method()).isEqualTo("HEAD");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }
}
