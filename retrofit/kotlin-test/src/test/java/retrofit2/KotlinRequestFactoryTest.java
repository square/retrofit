package retrofit2;

import static com.google.common.truth.Truth.assertThat;
import static retrofit2.TestingUtils.buildRequest;

import kotlin.Unit;
import okhttp3.Request;
import org.junit.Test;
import retrofit2.http.HEAD;

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
    assertThat(request.headers().size()).isEqualTo(0);
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }
}
