package retrofit2;

import static com.google.common.truth.Fact.simpleFact;
import static com.google.common.truth.Truth.assertAbout;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AnnotationArraySubject extends Subject {
  public static Factory<AnnotationArraySubject, Annotation[]> annotationArrays() {
    return AnnotationArraySubject::new;
  }

  public static AnnotationArraySubject assertThat(Annotation[] actual) {
    return assertAbout(annotationArrays()).that(actual);
  }

  private final List<Annotation> actual;

  private AnnotationArraySubject(FailureMetadata metadata, Annotation[] actual) {
    super(metadata, actual);
    this.actual = new ArrayList<>(actual.length);
    Collections.addAll(this.actual, actual);
  }

  public void hasAtLeastOneElementOfType(Class<? extends Annotation> cls) {
    for (Annotation annotation : actual) {
      if (cls.isAssignableFrom(annotation.annotationType())) {
        return;
      }
    }
    failWithActual(simpleFact("No annotations of instance " + cls));
  }
}
