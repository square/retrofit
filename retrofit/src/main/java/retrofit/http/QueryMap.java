/*
 * Copyright (C) 2014 Square, Inc.
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
package retrofit.http;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Query parameter keys and values appended to the URL.
 * <p>
 * Both keys and values are converted to strings using {@link String#valueOf(Object)}. Values are
 * URL encoded and {@code null} will not include the query parameter in the URL.
 * Passing an {@link java.lang.Iterable Iterable} or array as a value will result in a query
 * parameter for each non-{@code null} item.
 * <p>
 * Simple Example:
 * <pre>
 * &#64;GET("/search")
 * void list(@QueryMap Map&lt;String, String&gt; filters);
 * </pre>
 * Calling with {@code foo.list(ImmutableMap.of("foo", "bar", "kit", "kat"))} yields
 * {@code /search?foo=bar&kit=kat}.
 * <p>
 * Iterable example:
 * <pre>
 * &#64;GET("/search")
 * void list(@QueryMap Map&lt;String, Iterable&lt;String&gt;&gt; filters);
 * </pre>
 * Calling with {@code foo.list(ImmutableMap.of("foo", ImmutableSet.of("bar", "baz")))} yields
 * {@code /search?foo=bar&foo=baz}.
 *
 * @see Query
 * @see QueryMap
 * @see EncodedQueryMap
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface QueryMap {
}
