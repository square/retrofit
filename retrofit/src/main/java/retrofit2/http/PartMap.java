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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import retrofit2.Converter;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Denotes name and value parts of a multi-part request.
 * <p>
 * Values of the map on which this annotation exists will be processed in one of two ways:
 * <ul>
 * <li>If the type is {@link okhttp3.RequestBody RequestBody} the value will be used
 * directly with its content type.</li>
 * <li>Other object types will be converted to an appropriate representation by using
 * {@linkplain Converter a converter}.</li>
 * </ul>
 * <p>
 * <pre>{@code
 * &#64;Multipart
 * &#64;POST("/upload")
 * Call&lt;ResponseBody> upload(
 *     &#64;Part("file") RequestBody file,
 *     &#64;PartMap Map&lt;String, RequestBody&gt; params);
 * }</pre>
 *
 * @see Multipart
 * @see Part
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface PartMap {
  /** The {@code Content-Transfer-Encoding} of the parts. */
  String encoding() default "binary";

  /**
   * When true a {@code null} values will cause their part to be omitted. Otherwise {@code null}
   * values will be passed to the {@link Converter} for these parts.
   */
  boolean ignoreNull() default true;
}
