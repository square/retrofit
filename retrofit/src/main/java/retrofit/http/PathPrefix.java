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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Path prefix for all methods declared in an interface
 * <p>
 * Simple example:
 * <pre>
 * &#64;PathPrefix("/image")
 * public interface ImageService {
 *     &#64;GET("/{id}")
 *     void example(@Path("id") int id);
 * }
 * </pre>
 * Calling with {@code foo.example(1)} yields {@code /image/1}.
 * <p>
 * Path parameters may not be {@code null}.
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface PathPrefix {
  String value();
}
