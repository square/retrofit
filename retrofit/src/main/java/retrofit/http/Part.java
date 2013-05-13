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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Denotes a single part of a mutli-part request.
 * <p>
 * The parameter type on which this annotation exists will be processed in one of two ways:
 * <ul>
 * <li>If the type implements {@link retrofit.mime.TypedOutput TypedOutput} the headers and
 * body will be used directly.</li>
 * <li>Other object types will be converted to an appropriate representation by calling {@link
 * retrofit.converter.Converter#toBody(Object)}.</li>
 * </ul>
 * <p>
 * <pre>
 * &#64;Multipart
 * &#64;POST("/")
 * void example(&#64;Part("description") TypedString description,
 *              &#64;Part("image") TypedFile image,
 *              ...
 * );
 * </pre>
 */
@Target(PARAMETER) @Retention(RUNTIME)
public @interface Part {
  String value();
}
