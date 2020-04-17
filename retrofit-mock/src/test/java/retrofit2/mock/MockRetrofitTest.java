package retrofit2.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Test;
import retrofit2.Retrofit;

public final class MockRetrofitTest {
  private final Retrofit retrofit = new Retrofit.Builder().baseUrl("http://example.com").build();
  private final NetworkBehavior behavior = NetworkBehavior.create();
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  @Test
  public void retrofitNullThrows() {
    try {
      new MockRetrofit.Builder(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("retrofit == null");
    }
  }

  @Test
  public void retrofitPropagated() {
    MockRetrofit mockRetrofit = new MockRetrofit.Builder(retrofit).build();
    assertThat(mockRetrofit.retrofit()).isSameAs(retrofit);
  }

  @Test
  public void networkBehaviorNullThrows() {
    MockRetrofit.Builder builder = new MockRetrofit.Builder(retrofit);
    try {
      builder.networkBehavior(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("behavior == null");
    }
  }

  @Test
  public void networkBehaviorDefault() {
    MockRetrofit mockRetrofit = new MockRetrofit.Builder(retrofit).build();
    assertThat(mockRetrofit.networkBehavior()).isNotNull();
  }

  @Test
  public void networkBehaviorPropagated() {
    MockRetrofit mockRetrofit =
        new MockRetrofit.Builder(retrofit).networkBehavior(behavior).build();
    assertThat(mockRetrofit.networkBehavior()).isSameAs(behavior);
  }

  @Test
  public void backgroundExecutorNullThrows() {
    MockRetrofit.Builder builder = new MockRetrofit.Builder(retrofit);
    try {
      builder.backgroundExecutor(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("executor == null");
    }
  }

  @Test
  public void backgroundExecutorDefault() {
    MockRetrofit mockRetrofit = new MockRetrofit.Builder(retrofit).build();
    assertThat(mockRetrofit.backgroundExecutor()).isNotNull();
  }

  @Test
  public void backgroundExecutorPropagated() {
    MockRetrofit mockRetrofit =
        new MockRetrofit.Builder(retrofit).backgroundExecutor(executor).build();
    assertThat(mockRetrofit.backgroundExecutor()).isSameAs(executor);
  }
}
