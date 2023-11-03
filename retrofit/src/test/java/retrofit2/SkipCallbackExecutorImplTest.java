package retrofit2;

import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.annotation.Annotation;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class SkipCallbackExecutorImplTest {

  private static SkipCallbackExecutor INSTANCE;

  @BeforeClass
  public static void setUp() {
    INSTANCE = new SkipCallbackExecutorImpl();
  }

  @Test
  public void testEnsurePresent() {

    final SkipCallbackExecutor INSTANCE = new SkipCallbackExecutorImpl();

    // Test case 1: Annotations already contain SkipCallbackExecutor
    Annotation[] annotationsWithSkipCallbackExecutor = {new SkipCallbackExecutorImpl()};
    Annotation[] result1 = SkipCallbackExecutorImpl.ensurePresent(annotationsWithSkipCallbackExecutor);
    assertArrayEquals(annotationsWithSkipCallbackExecutor, result1);

    // Test case 2: Annotations don't contain SkipCallbackExecutor
    Annotation[] annotationsWithoutSkipCallbackExecutor = {};
    Annotation[] result2 = SkipCallbackExecutorImpl.ensurePresent(annotationsWithoutSkipCallbackExecutor);
    assertEquals(annotationsWithoutSkipCallbackExecutor.length + 1, result2.length);
    assertEquals(INSTANCE, result2[0]);
  }

  @Test
  public void testHashCode() {
    int hashCode = INSTANCE.hashCode();
    assertEquals(0, hashCode);
  }

  @Test
  public void testToString() {
    String expectedToString = "@" + SkipCallbackExecutor.class.getName() + "()";
    assertEquals(expectedToString, INSTANCE.toString());
  }

  @Test
  public void testAnnotationType() {
    // Use customInstance in your test cases instead of INSTANCE
    Class<? extends Annotation> annotationType = INSTANCE.annotationType();
    assertEquals(SkipCallbackExecutor.class, annotationType);
  }

}
