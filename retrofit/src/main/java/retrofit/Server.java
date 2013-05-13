/*
 * Copyright (C) 2010 Square, Inc.
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

/**
 * Represents an API endpoint URL and associated name. Callers should always consult the instance
 * for the latest values rather than caching the returned values.
 *
 * @author Bob Lee (bob@squareup.com)
 * @see ChangeableServer
 */
public class Server {
  public static final String DEFAULT_NAME = "default";

  private final String apiUrl;
  private final String type;

  /** Create a server with the provided URL and default name. */
  public Server(String apiUrl) {
    this(apiUrl, DEFAULT_NAME);
  }

  /** Create a server with the provided URL and name. */
  public Server(String apiUrl, String type) {
    this.apiUrl = apiUrl;
    this.type = type;
  }

  /** The base API URL. */
  public String getUrl() {
    return apiUrl;
  }

  /** A name for differentiating between multiple API URLs. */
  public String getName() {
    return type;
  }
}
