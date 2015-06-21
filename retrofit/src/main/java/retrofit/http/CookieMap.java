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

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Add to the cookie header a list of cookies with the the key-value of its
 * target Map.
 * <p>
 * Both keys (cookie name) and values (cookie value) are converted to strings
 * using {@link String#valueOf(Object)}. {@code null} keys are not allowed.
 *
 * <pre>
 * &#064;GET(&quot;/&quot;)
 * void fooBar(@CookieMap Map&lt;String, String&gt; cookies);
 * </pre>
 * <p>
 * CookieMap values may be {@code null} which will omit them from the request.
 * Passing a {@link java.util.Map Map} will result in a cookie for each not-
 * {@code null} item.
 * <p>
 * <strong>Note:</strong> Cookies do not overwrite each other. All cookies with
 * the same name will be included in the request.
 *
 * @author Filippo Buletto (filippo.buletto@gmail.com)
 *
 */
@Documented
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface CookieMap {

}
