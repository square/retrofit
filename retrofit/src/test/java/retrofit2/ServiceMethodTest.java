package retrofit2;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class ServiceMethodTest {

    @Test
    public void testParseAnnotations() {
        // Setup
        final Retrofit retrofit = null;
        final Method method = null;

        // Run the test
        final ServiceMethod<Object> result = ServiceMethod.parseAnnotations(retrofit, method);
        assertEquals(null, result.invoke(new Object[]{"args"}));
    }
}
