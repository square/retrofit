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

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import retrofit2.Retrofit;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Named pair for a SimpleJSON request.
 * <p>
 * Values are converted to strings using {@link Retrofit#stringConverter(Type, Annotation[])}
 * (or {@link Object#toString()}, if no matching string converter is installed)
 * and then form URL encoded.
 * {@code null} values are ignored. value's type can only be raw type.
 * <p>
 * Simple Example:
 * <pre><code>
 * &#64;SimpleJSON
 * &#64;POST("/login")
 * Call&lt;ResponseBody&gt; login(
 *     &#64;SimpleJSONField("username") String username,
 *     &#64;SimpleJSONField("password") String password);
 * </code></pre>
 * Calling with {@code foo.example("Bob Smith", "President")} yields a request body of
 * {@code {'password':'Bob Smith','password':'President'}}.
 * <p>
 *
 * @see SimpleJSON
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface SimpleJSONField {
    /** The SimpleJSON filed name. */
    String value();
}
