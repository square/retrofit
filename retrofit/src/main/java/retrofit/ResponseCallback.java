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
 * An extension of {@link Callback} which returns only {@link Response} object
 * in {@link Callback#success(Object, retrofit.client.Response)} method.
 */
public abstract class ResponseCallback implements Callback<Response> {

  @Override public void success(Response response, Response response2) {
    success(response);
  }

  /** Successful HTTP response. */
  public abstract void success(Response response);
}
