package retrofit2;

import kotlin.Unit;
import okhttp3.Request;
import org.junit.Test;

import retrofit2.http.BaseUrl;
import retrofit2.http.HEAD;

import static org.assertj.core.api.Assertions.assertThat;
import static retrofit2.TestingUtils.buildRequest;

public final class KotlinRequestFactoryTest {
  @Test
  public void headUnit() {
    Request request = buildRequest(Remote.class);
    assertThat(request.method()).isEqualTo("HEAD");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  @Test
  public void overrideBaseUrl() {
    @BaseUrl("http://example2.com/")
    class Remote2 {
      @HEAD("/foo/bar/")
      Call<Unit> method() {
        return null;
      }
    }

    Request request = buildRequest(Remote2.class);
    assertThat(request.method()).isEqualTo("HEAD");
    assertThat(request.headers().size()).isZero();
    assertThat(request.url().toString()).isEqualTo("http://example2.com/foo/bar/");
    assertThat(request.body()).isNull();
  }

  static class Remote {
    @HEAD("/foo/bar/")
    Call<Unit> method() {
      return null;
    }
  }
}
