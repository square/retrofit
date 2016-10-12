/*
 * Copyright (C) 2016 Square, Inc.
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
import okhttp3.Call;

/**
 * A factory of HTTP clients for service methods.
 * <p>
 * Instances of this class can be used to customize properties the HTTP client for certain methods
 * in your service interface. For example, an implementation could use the presence of a
 * {@code @GzipRequest} annotation to add an interceptor which applies GZIP to the request body.
 */
public abstract class ClientFactory {
  /**
   * Returns a {@link Call.Factory} for use on a method with {@code annotations}. Implementations
   * are not required to return a new instance for each invocation.
   */
  public abstract Call.Factory forCall(Annotation[] annotations);
}
