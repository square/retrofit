package retrofit2;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class HttpServiceMethodTest {

    @Test
    public void testParseAnnotations() {
        // Setup
        final Retrofit retrofit = null;
        final Method method = null;
        final RequestFactory requestFactory = null;

        // Run the test
        final HttpServiceMethod<Object, Object> result = HttpServiceMethod.parseAnnotations(retrofit, method,
                requestFactory);
        assertEquals(null, result.invoke(new Object[]{"args"}));
    }
}
