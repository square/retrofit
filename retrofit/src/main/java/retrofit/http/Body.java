/*
 * Copyright (C) 2011 Square, Inc.
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
 * Use this annotation on a service method param when you want to directly control the request body
 * of a POST/PUT request (instead of sending in as request parameters or form-style request
 * body).  If the value of the parameter implements TypedOutput, the request body will be written
 * exactly as specified by the TypedOutput.writeTo object.  If it doesn't implement TypedOutput, the
 * object will be serialized into JSON and the result will be set directly as the request body.
 *
 * @author Eric Denman (edenman@squareup.com)
 */
@Target(PARAMETER) @Retention(RUNTIME)
public @interface Body {
}
