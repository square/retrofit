package retrofit;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CallableRequestBuilderTest {
  @Test public void testTypes() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com")
        .addConverterFactory(new StringListConverterFactory())
        .build();

    Type responseType = new TypeToken<List<String>>(getClass()) { }.getType();
    CallableRequest request = new CallableRequest.Builder(retrofit)
        .path("/")
        .responseType(responseType)
        .method(Method.GET)
        .build();

    Type returnType = request.returnType();
    assertThat(returnType).isInstanceOf(ParameterizedType.class);
    ParameterizedType parameterizedType = (ParameterizedType) returnType;
    assertThat(parameterizedType.getRawType()).isEqualTo(Call.class);
    assertThat(parameterizedType.getActualTypeArguments()[0]).isEqualTo(responseType);
  }

  @Test public void newBuilder() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com")
        .addConverterFactory(new StringListConverterFactory())
        .build();

    CallableRequest request = new CallableRequest.Builder(retrofit)
        .path("/")
        .responseType(
            new TypeToken<List<String>>(CallableRequestBuilderTest.this.getClass()) {
            }.getType())
        .method(Method.GET)
        .build();

    ImmutableList<Query> params = ImmutableList.of(new Query("foo", "bar"));
    ImmutableList<Part> parts = ImmutableList.of(new Part("kit", "kat"));
    CallableRequest newRequest = request.newBuilder(retrofit)
        .path("/abc")
        .method(Method.POST)
        .queryParams(params)
        .body(123)
        .parts(parts)
        .build();

    assertThat(newRequest.path()).isEqualTo("/abc");
    assertThat(newRequest.method()).isEqualTo(Method.POST);
    assertThat(newRequest.queryParams()).isEqualTo(params);
    assertThat(newRequest.body()).isEqualTo(123);
    assertThat(newRequest.parts()).isEqualTo(parts);
  }
}