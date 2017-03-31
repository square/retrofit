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
 * Query parameter appended to the URL that has no value.
 * <p>
 * Passing a {@link java.util.List List} or array will result in a query parameter for each
 * non-{@code null} item.
 * <p>
 * Simple Example:
 * <pre><code>
 * &#64;GET("/friends")
 * Call&lt;ResponseBody&gt; friends(@QueryName String filter);
 * </code></pre>
 * Calling with {@code foo.friends("contains(Bob)")} yields {@code /friends?contains(Bob)}.
 * <p>
 * Array/Varargs Example:
 * <pre><code>
 * &#64;GET("/friends")
 * Call&lt;ResponseBody&gt; friends(@QueryName String... filters);
 * </code></pre>
 * Calling with {@code foo.friends("contains(Bob)", "age(42)")} yields
 * {@code /friends?contains(Bob)&age(42)}.
 * <p>
 * Parameter names are URL encoded by default. Specify {@link #encoded() encoded=true} to change
 * this behavior.
 * <pre><code>
 * &#64;GET("/friends")
 * Call&lt;ResponseBody&gt; friends(@QueryName(encoded=true) String filter);
 * </code></pre>
 * Calling with {@code foo.friends("name+age"))} yields {@code /friends?name+age}.
 *
 * @see Query
 * @see QueryMap
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface QueryName {
  /**
   * Specifies whether the parameter is already URL encoded.
   */
  boolean encoded() default false;
}
