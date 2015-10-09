/*
 * Copyright (C) 2015 Square, Inc.
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
 * Denotes a single {@link java.io.File} part of a multi-part request.
 * <p>
 * Values may be {@code null} which will omit them from the request body.
 * <p>
 * The {@code Content-Transfer-Encoding} will be {@code binary}. The {@code Content-Type} and
 * {@code filename} parameter will both be read from the {@code File} provided.
 * <p>
 * <pre>
 * &#64;Multipart
 * &#64;POST("/")
 * Call&lt;ResponseBody&gt; upload(
 *     &#64;PartFile("image") File imageFile);
 * </pre>
 * <p>
 * PartFile parameters may not be {@code null}.
 *
 * @see Multipart
 * @see Part
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface PartFile {
  String value();
}
