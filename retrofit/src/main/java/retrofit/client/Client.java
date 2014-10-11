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
package retrofit.client;

import java.io.IOException;

/**
 * Abstraction of an HTTP client which can execute {@link Request Requests}. This class must be
 * thread-safe as invocation may happen from multiple threads simultaneously.
 */
public interface Client {
  /**
   * Synchronously execute an HTTP represented by {@code request} and encapsulate all response data
   * into a {@link Response} instance.
   * <p>
   * Note: If the request has a body, its length and mime type will have already been added to the
   * header list as {@code Content-Length} and {@code Content-Type}, respectively. Do NOT alter
   * these values as they might have been set as a result of an application-level configuration.
   */
  Response execute(Request request) throws IOException;
}
