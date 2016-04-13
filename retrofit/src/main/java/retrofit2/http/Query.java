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
 * Query parameter appended to the URL.
 * <p>
 * Values are converted to strings using {@link String#valueOf(Object)} and then URL encoded.
 * {@code null} values are ignored. Passing a {@link java.util.List List} or array will result in a
 * query parameter for each non-{@code null} item.
 * <p>
 * Simple Example:
 * <pre><code>
 * &#64;GET("/list")
 * Call&lt;ResponseBody&gt; list(@Query("page") int page);
 * </code></pre>
 * Calling with {@code foo.list(1)} yields {@code /list?page=1}.
 * <p>
 * Example with {@code null}:
 * <pre><code>
 * &#64;GET("/list")
 * Call&lt;ResponseBody&gt; list(@Query("category") String category);
 * </code></pre>
 * Calling with {@code foo.list(null)} yields {@code /list}.
 * <p>
 * Array/Varargs Example:
 * <pre><code>
 * &#64;GET("/list")
 * Call&lt;ResponseBody&gt; list(@Query("category") String... categories);
 * </code></pre>
 * Calling with {@code foo.list("bar", "baz")} yields
 * {@code /list?category=bar&category=baz}.
 * <p>
 * Parameter names and values are URL encoded by default. Specify {@link #encoded() encoded=true}
 * to change this behavior.
 * <pre><code>
 * &#64;GET("/search")
 * Call&lt;ResponseBody&gt; list(@Query(value="foo", encoded=true) String foo);
 * </code></pre>
 * Calling with {@code foo.list("foo+bar"))} yields {@code /search?foo=foo+bar}.
 *
 * @see QueryMap
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Query {
  /** The query parameter name. */
  String value();

  /**
   * Specifies whether the parameter {@linkplain #value() name} and value are already URL encoded.
   */
  boolean encoded() default false;
}
