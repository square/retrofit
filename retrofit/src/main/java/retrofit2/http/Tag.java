/*
 * Copyright (C) 2019 Square, Inc.
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
 * Adds the argument instance as a request tag using the type as the key.
 *
 * <pre><code>
 * &#64;GET("/")
 * Call&lt;ResponseBody&gt; foo(@Tag String tag);
 * </code></pre>
 *
 * Tag arguments may be {@code null} which will omit them from the request. Passing a parameterized
 * type such as {@code List<String>} will use the raw type (i.e., {@code List.class}) as the key.
 * Duplicate tag types are not allowed.
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Tag {}
