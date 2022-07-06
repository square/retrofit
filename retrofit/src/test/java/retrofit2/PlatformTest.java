package retrofit2;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class PlatformTest {

    @Test
    public void testGet() throws Throwable {
        // Run the test
        final Platform result = Platform.get();
        assertEquals(MoreExecutors.directExecutor(), result.defaultCallbackExecutor());
        final Executor callbackExecutor = MoreExecutors.directExecutor();
        assertEquals(Arrays.asList(), result.createDefaultCallAdapterFactories(callbackExecutor));
        assertEquals(Arrays.asList(), result.createDefaultConverterFactories());
        final Method method = null;
        assertFalse(result.isDefaultMethod(method));
        final Method method1 = null;
        assertEquals("result", result.invokeDefaultMethod(method1, Object.class, "proxy", "args"));
    }
}
