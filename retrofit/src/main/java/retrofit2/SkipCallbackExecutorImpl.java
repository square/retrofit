/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit2;

import java.lang.annotation.Annotation;

// This class conforms to the annotation requirements documented on Annotation.
final class SkipCallbackExecutorImpl implements SkipCallbackExecutor {
  private static final SkipCallbackExecutor INSTANCE = new SkipCallbackExecutorImpl();

  static Annotation[] ensurePresent(Annotation[] annotations) {
    if (Utils.isAnnotationPresent(annotations, SkipCallbackExecutor.class)) {
      return annotations;
    }

    Annotation[] newAnnotations = new Annotation[annotations.length + 1];
    // Place the skip annotation first since we're guaranteed to check for it in the call adapter.
    newAnnotations[0] = SkipCallbackExecutorImpl.INSTANCE;
    System.arraycopy(annotations, 0, newAnnotations, 1, annotations.length);
    return newAnnotations;
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    return SkipCallbackExecutor.class;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof SkipCallbackExecutor;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public String toString() {
    return "@" + SkipCallbackExecutor.class.getName() + "()";
  }
}
