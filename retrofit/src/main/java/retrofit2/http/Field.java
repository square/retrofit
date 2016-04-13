/*
 * Copyright (C) 2013 Square, Inc.
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
package retrofit2.http;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Named pair for a form-encoded request.
 * <p>
 * Values are converted to strings using {@link String#valueOf(Object)} and then form URL encoded.
 * {@code null} values are ignored. Passing a {@link java.util.List List} or array will result in a
 * field pair for each non-{@code null} item.
 * <p>
 * Simple Example:
 * <pre><code>
 * &#64;FormUrlEncoded
 * &#64;POST("/")
 * Call&lt;ResponseBody&gt; example(
 *     &#64;Field("name") String name,
 *     &#64;Field("occupation") String occupation);
 * </code></pre>
 * Calling with {@code foo.example("Bob Smith", "President")} yields a request body of
 * {@code name=Bob+Smith&occupation=President}.
 * <p>
 * Array/Varargs Example:
 * <pre><code>
 * &#64;FormUrlEncoded
 * &#64;POST("/list")
 * Call&lt;ResponseBody&gt; example(@Field("name") String... names);
 * </code></pre>
 * Calling with {@code foo.example("Bob Smith", "Jane Doe")} yields a request body of
 * {@code name=Bob+Smith&name=Jane+Doe}.
 *
 * @see FormUrlEncoded
 * @see FieldMap
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Field {
  String value();

  /** Specifies whether the {@linkplain #value() name} and value are already URL encoded. */
  boolean encoded() default false;
}
