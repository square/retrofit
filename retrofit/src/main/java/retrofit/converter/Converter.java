/*
 * Copyright (C) 2012 Square, Inc.
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
package retrofit.converter;

import java.lang.reflect.Type;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/**
 * Arbiter for converting objects to and from their representation in HTTP.
 *
 * @author Jake Wharton (jw@squareup.com)
 */
public interface Converter {
  /**
   * Convert an HTTP response body to a concrete object of the specified type.
   *
   * @param body HTTP response body.
   * @param type Target object type.
   * @return Instance of {@code type} which will be cast by the caller.
   * @throws ConversionException if conversion was unable to complete. This will trigger a call to
   * {@link retrofit.Callback#failure(retrofit.RetrofitError)} or throw a
   * {@link retrofit.RetrofitError}. The exception message should report all necessary information
   * about its cause as the response body will be set to {@code null}.
   */
  Object fromBody(TypedInput body, Type type) throws ConversionException;

  /**
   * Convert an object to an appropriate representation for HTTP transport.
   *
   * @param object Object instance to convert.
   * @return Representation of the specified object as bytes.
   */
  TypedOutput toBody(Object object);
}
