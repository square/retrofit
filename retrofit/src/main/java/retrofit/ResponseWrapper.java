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
package retrofit;

import retrofit.client.Response;

/**
 * A wrapper that holds the {@link Response} and {@link retrofit.converter.Converter} response to
 * be used by the {@link CallbackRunnable} for success method calls on {@link Callback}.
 *
 * @author JJ Ford (jj.n.ford@gmail.com)
 */
final class ResponseWrapper {
  final Response response;
  final Object responseBody;

  ResponseWrapper(Response response, Object responseBody) {
    this.response = response;
    this.responseBody = responseBody;
  }
}
