/*
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
 * Denotes the root key of a JSON request.
 * <p>
 * Simple Example:
 * <pre><code>
 * &#64;JSON
 * &#64;POST("/")
 * Call&lt;ResponseBody&gt; example(
 *     &#64;Root("name") String yourName);
 * </code></pre>
 * Calling with {@code foo.example("Bob Smith")} yields a request body of
 * <code>{name=>"Bob Smith"}</code>.
 * <p>
 * Array Example:
 * <pre><code>
 * &#64;JSON
 * &#64;POST("/")
 * Call&lt;ResponseBody&gt; example(
 *     &#64;Root("name") List<String> names);
 * </code></pre>
 * Calling with {@code foo.example("Bob Smith", "Jane Doe")} yields a request body of
 * <code>{name=>["Bob Smith", "Jane Doe"]}</code>.
 * @see JSON
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Root {
  /**
   * The value of the JSON root.
   * Results in {"value" : object}
   */
  String value();
}
