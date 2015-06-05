package retrofit2;

import com.google.common.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import rx.Observable;

import static org.assertj.core.api.Assertions.assertThat;

public class ObservableRequestBuilderTest {
  @Test public void testTypes() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://example.com")
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .addConverterFactory(new StringListConverterFactory())
        .build();

    Type responseType = new TypeToken<List<String>>(getClass()) { }.getType();
    ObservableRequest request = new ObservableRequest.Builder(retrofit)
        .path("/")
        .responseType(responseType)
        .method(Method.GET)
        .build();

    Type returnType = request.returnType();
    assertThat(returnType).isInstanceOf(ParameterizedType.class);
    ParameterizedType parameterizedType = (ParameterizedType) returnType;
    assertThat(parameterizedType.getRawType()).isEqualTo(Observable.class);
    assertThat(parameterizedType.getActualTypeArguments()[0]).isEqualTo(responseType);
  }
}
