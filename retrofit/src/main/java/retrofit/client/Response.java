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

import retrofit.mime.TypedInput;

import java.util.List;

/**
 * An HTTP response.
 * <p>
 * When used directly as a data type for an interface method, the response body is buffered to a
 * {@code byte[]}. Annotate the method with {@link retrofit.http.Streaming @Streaming} for an
 * unbuffered stream from the network.
 */
public final class Response extends AbstractResponse<TypedInput> {
  public Response(String url, int status, String reason, List<Header> headers, TypedInput body) {
    super(url, status, reason, headers, body);
  }
}
