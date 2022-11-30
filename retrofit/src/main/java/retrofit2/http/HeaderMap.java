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
package retrofit2.http;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.Map;
import retrofit2.Retrofit;

/**
 * Adds headers specified in the {@link Map} or {@link okhttp3.Headers}.
 *
 * <p>Values in the map are converted to strings using {@link Retrofit#stringConverter(Type,
 * Annotation[])} (or {@link Object#toString()}, if no matching string converter is installed).
 *
 * <p>Simple Example:
 *
 * <pre>
 * &#64;GET("/search")
 * void list(@HeaderMap Map&lt;String, String&gt; headers);
 *
 * ...
 *
 * // The following call yields /search with headers
 * // Accept: text/plain and Accept-Charset: utf-8
 * foo.list(ImmutableMap.of("Accept", "text/plain", "Accept-Charset", "utf-8"));
 * </pre>
 *
 * <p>Map keys and values representing parameter values allow only ascii values by default.
 * Specify {@link #allowUnsafeNonAsciiValues() allowUnsafeNonAsciiValues=true} to change this behavior.
 *
 * <pre>
 * &#64;GET("/search")
 * void list(@HeaderMap(allowUnsafeNonAsciiValues=true) Map&lt;String, String&gt; headers);
 *
 * @see Header
 * @see Headers
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface HeaderMap {
  /** Specifies whether the parameter values are allowed with unsafe non ascii values. */
  boolean allowUnsafeNonAsciiValues() default false;
}
