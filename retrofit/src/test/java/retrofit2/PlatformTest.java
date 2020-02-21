package retrofit2;

import org.junit.Test;

import java.lang.reflect.Method;

public class PlatformTest {
    private interface DoNothing {
        default void doNothing() { }
    }

    @Test public void testDefaultMethodInvoke() throws Throwable {
        Object doNothing = new DoNothing() {};
        Platform p = new Platform(true);
        Method doNothingMethod = DoNothing.class.getMethod("doNothing");
        p.invokeDefaultMethod(doNothingMethod, DoNothing.class, doNothing);
    }
}