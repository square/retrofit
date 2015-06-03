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
package retrofit.http;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Denotes a single part of a multi-part request.
 * <p>
 * The parameter type on which this annotation exists will be processed in one of three ways:
 * <ul>
 * <li>If the type is {@link String} the value will also be used directly with a {@code text/plain}
 * content type.</li>
 * <li>Other object types will be converted to an appropriate representation by calling {@link
 * retrofit.Converter#toBody(Object, java.lang.reflect.Type)}.</li>
 * </ul>
 * <p>
 * Values may be {@code null} which will omit them from the request body.
 * <p>
 * <pre>
 * &#64;Multipart
 * &#64;POST("/")
 * void example(&#64;Part("description") String description,
 *              &#64;Part("image") TypedFile image,
 *              ...
 * );
 * </pre>
 * <p>
 * Part parameters may not be {@code null}.
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Part {
  String value();
  /** The {@code Content-Transfer-Encoding} of this part. */
  String encoding() default "binary";
}
