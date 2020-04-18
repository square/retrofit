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
package retrofit2.http;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Named key/value pairs for a form-encoded request.
 *
 * <p>Simple Example:
 *
 * <pre><code>
 * &#64;FormUrlEncoded
 * &#64;POST("/things")
 * Call&lt;ResponseBody&gt; things(@FieldMap Map&lt;String, String&gt; fields);
 * </code></pre>
 *
 * Calling with {@code foo.things(ImmutableMap.of("foo", "bar", "kit", "kat")} yields a request body
 * of {@code foo=bar&kit=kat}.
 *
 * <p>A {@code null} value for the map, as a key, or as a value is not allowed.
 *
 * @see FormUrlEncoded
 * @see Field
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface FieldMap {
  /** Specifies whether the names and values are already URL encoded. */
  boolean encoded() default false;
}
