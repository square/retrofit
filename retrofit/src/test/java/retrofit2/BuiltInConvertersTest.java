package retrofit2;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class BuiltInConvertersTest {

    private BuiltInConverters builtInConvertersUnderTest;

    @Before
    public void setUp() {
        builtInConvertersUnderTest = new BuiltInConverters();
    }

    @Test
    public void testResponseBodyConverter() {
        // Setup
        final Type type = null;
        final Annotation[] annotations = new Annotation[]{};

        // Run the test
        final Converter<ResponseBody, ?> result = builtInConvertersUnderTest.responseBodyConverter(type, annotations,
                null);

        // Verify the results
    }

    @Test
    public void testRequestBodyConverter() {
        // Setup
        final Type type = null;
        final Annotation[] parameterAnnotations = new Annotation[]{};
        final Annotation[] methodAnnotations = new Annotation[]{};

        // Run the test
        final Converter<?, RequestBody> result = builtInConvertersUnderTest.requestBodyConverter(type,
                parameterAnnotations, methodAnnotations, null);

        // Verify the results
    }
}
