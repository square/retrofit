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
 * Denotes name and value parts of a multi-part request
 * <p>
 * Values of the map on which this annotation exists will be processed in one of three ways:
 * <ul>
 * <li>If the type is {@link String} the value will also be used directly with a {@code text/plain}
 * content type.</li>
 * <li>Other object types will be converted to an appropriate representation by calling {@link
 * retrofit.Converter#toBody(Object, java.lang.reflect.Type)}.</li>
 * </ul>
 * <p>
 * <pre>
 * &#64;Multipart
 * &#64;POST("/upload")
 * void upload(&#64;Part("file") TypedFile file, &#64;PartMap Map&lt;String, String&gt; params);
 * </pre>
 *
 * @see Multipart
 * @see Part
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface PartMap {
  /** The {@code Content-Transfer-Encoding} of this part. */
  String encoding() default "binary";
}
