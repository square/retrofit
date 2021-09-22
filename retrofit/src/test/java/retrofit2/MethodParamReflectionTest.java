package retrofit2;

import org.junit.Test;

import okhttp3.ResponseBody;
import retrofit2.http.GET;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class MethodParamReflectionTest {

    interface Example {
        @GET("/") //
        Call<ResponseBody> method(String theFirstParameter);
    }

    @Test
    public void paramIndexIsUsedWithoutParamReflection() {

        // TODO: Inject a Platform where method param reflection is unavailable
        Retrofit retrofit =  new Retrofit.Builder()
                .baseUrl("http://example.com/")
                .callFactory(request -> {
                    throw new UnsupportedOperationException("Not implemented");
                })
                .validateEagerly(true)
                .build();

        try {
            retrofit.create(Example.class);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e)
                    .hasMessage(
                            "No Retrofit annotation found. (parameter #1)\n    for method Example.method");
        }
    }

    @Test
    public void paramNameIsUsedWithParamReflection() {

        // TODO: Inject a Platform where method param reflection is available
        Retrofit retrofit =  new Retrofit.Builder()
                .baseUrl("http://example.com/")
                .callFactory(request -> {
                    throw new UnsupportedOperationException("Not implemented");
                })
                .validateEagerly(true)
                .build();

        try {
            retrofit.create(Example.class);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e)
                    .hasMessage(
                            "No Retrofit annotation found. (parameter 'theFirstParameter')\n    for method Example.method");
        }
    }
}
