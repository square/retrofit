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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Named replacement in a header segment. Values are converted to string using
 * {@link String#valueOf(Object)}
 * <p>
 * Simple example:
 *  @Headers({"Range:byte={start}-{end}"})
 *
 *
 * test(@Head("start")String start,@Head("end")String end) to replace like @Path repace.
 *
 */
@Documented
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface Head {
  String value();

}
