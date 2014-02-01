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

/**
 * An {@link Endpoint} whose URL and name can be changed at runtime.
 *
 * @deprecated Implement {@link Endpoint} to change URL and name at runtime. This class will be
 * removed in version 1.5.
 */
@Deprecated
public class ChangeableServer implements Endpoint {
  private String url;
  private String name;

  /** Create a changeable server with the provided URL and default name. */
  public ChangeableServer(String url) {
    this.url = url;
    this.name = Server.DEFAULT_NAME;
  }

  /** Create a changeable server with the provided URL and name. */
  public ChangeableServer(String url, String name) {
    this.url = url;
    this.name = name;
  }

  /** Update the URL returned by {@link #getUrl()}. */
  public void update(String url) {
    this.url = url;
  }

  /** Update the URL and name returned by {@link #getUrl()} and {@link #getName()}, respectively. */
  public void update(String url, String name) {
    this.url = url;
    this.name = name;
  }

  @Override public String getUrl() {
    return url;
  }

  @Override public String getName() {
    return name;
  }
}
