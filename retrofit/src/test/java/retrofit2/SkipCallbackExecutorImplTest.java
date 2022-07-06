package retrofit2;

import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.Annotation;

import static org.junit.Assert.*;

public class SkipCallbackExecutorImplTest {

    private SkipCallbackExecutorImpl skipCallbackExecutorImplUnderTest;

    @Before
    public void setUp() {
        skipCallbackExecutorImplUnderTest = new SkipCallbackExecutorImpl();
    }

    @Test
    public void testAnnotationType() {
        assertEquals(Object.class, skipCallbackExecutorImplUnderTest.annotationType());
    }

    @Test
    public void testEquals() {
        assertFalse(skipCallbackExecutorImplUnderTest.equals("obj"));
    }

    @Test
    public void testHashCode() {
        assertEquals(0, skipCallbackExecutorImplUnderTest.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("result", skipCallbackExecutorImplUnderTest.toString());
    }

    @Test
    public void testEnsurePresent() {
        // Setup
        final Annotation[] annotations = new Annotation[]{};
        final Annotation[] expectedResult = new Annotation[]{};

        // Run the test
        final Annotation[] result = SkipCallbackExecutorImpl.ensurePresent(annotations);

        // Verify the results
        assertArrayEquals(expectedResult, result);
    }
}
