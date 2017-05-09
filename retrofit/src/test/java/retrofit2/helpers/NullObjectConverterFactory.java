package retrofit2.helpers;

import retrofit2.Converter;
import retrofit2.Retrofit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Always converts strings to null
 */
public final class NullObjectConverterFactory extends ToStringConverterFactory {
    @Override
    public Converter<?, String> stringConverter(final Type type, final Annotation[] annotations, final Retrofit retrofit) {
        return new Converter<Object, String>() {
            @Override
            public String convert(final Object value) throws IOException {
                return null;
            }
        };
    }
}
