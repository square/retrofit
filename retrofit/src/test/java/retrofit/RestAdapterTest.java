// Copyright 2013 Square, Inc.
package retrofit;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import retrofit.http.GET;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class RestAdapterTest {
  interface CallMethod {
    @GET("/") Call<String> method();
  }
  interface FutureMethod {
    @GET("/") Future<String> method();
  }
  interface Extending extends CallMethod {
  }

  @SuppressWarnings("EqualsBetweenInconvertibleTypes") // We are explicitly testing this behavior.
  @Test public void objectMethodsStillWork() {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint("http://example.com")
        .build();
    CallMethod example = ra.create(CallMethod.class);

    assertThat(example.hashCode()).isNotZero();
    assertThat(example.equals(this)).isFalse();
    assertThat(example.toString()).isNotEmpty();
  }

  @Test public void interfaceWithExtendIsNotSupported() {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint("http://example.com")
        .build();
    try {
      ra.create(Extending.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Interface definitions must not extend other interfaces.");
    }
  }

  @Test public void callReturnTypeAdapterAddedByDefault() {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint("http://example.com")
        .build();
    CallMethod example = ra.create(CallMethod.class);
    assertThat(example.method()).isInstanceOf(Call.class);
  }

  @Test public void callReturnTypeCustomAdapter() {
    final AtomicBoolean factoryCalled = new AtomicBoolean();
    final AtomicBoolean adapterCalled = new AtomicBoolean();
    class MyCallAdapterFactory implements CallAdapter.Factory {
      @Override public CallAdapter<?> get(final Type returnType) {
        factoryCalled.set(true);
        if (Utils.getRawType(returnType) != Call.class) {
          return null;
        }
        return new CallAdapter<Object>() {
          @Override public Type responseType() {
            return Utils.getSingleParameterUpperBound((ParameterizedType) returnType);
          }

          @Override public Object adapt(Call<Object> call) {
            adapterCalled.set(true);
            return call;
          }
        };
      }
    }

    RestAdapter ra = new RestAdapter.Builder()
        .endpoint("http://example.com")
        .addCallAdapterFactory(new MyCallAdapterFactory())
        .build();
    CallMethod example = ra.create(CallMethod.class);
    assertThat(example.method()).isInstanceOf(Call.class);
    assertThat(factoryCalled.get()).isTrue();
    assertThat(adapterCalled.get()).isTrue();
  }

  interface StringService {
    @GET("/") String get();
  }

  @Test public void customReturnTypeAdapter() {
    class GreetingCallAdapterFactory implements CallAdapter.Factory {
      @Override public CallAdapter<?> get(Type returnType) {
        if (Utils.getRawType(returnType) != String.class) {
          return null;
        }
        return new CallAdapter<Object>() {
          @Override public Type responseType() {
            return Object.class;
          }

          @Override public String adapt(Call<Object> call) {
            return "Hi!";
          }
        };
      }
    }

    RestAdapter ra = new RestAdapter.Builder()
        .endpoint("http://example.com")
        .addCallAdapterFactory(new GreetingCallAdapterFactory())
        .build();
    StringService example = ra.create(StringService.class);
    assertThat(example.get()).isEqualTo("Hi!");
  }

  @Test public void customReturnTypeAdapterMissingThrows() {
    RestAdapter ra = new RestAdapter.Builder()
        .endpoint("http://example.com")
        .build();
    FutureMethod example = ra.create(FutureMethod.class);
    try {
      example.method();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("FutureMethod.method: No registered call adapters were able to "
          + "handle return type java.util.concurrent.Future<java.lang.String>. "
          + "Checked: [Built-in CallAdapterFactory]");
    }
  }
}
