/*
 * Copyright (C) 2020 Square, Inc.
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
package retrofit2;

public final class RoboVmPlatformTest {
  public static void main(String[] args) {
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl("https://example.com")
      .callFactory(c -> { throw new AssertionError(); })
      .build();

    if (retrofit.callAdapterFactories().size() > 1) {
      // Everyone gets the callback executor adapter. If RoboVM was correctly detected it will NOT
      // get the Java 8-supporting CompletableFuture call adapter factory.
      System.exit(1);
    }
  }

  private RoboVmPlatformTest() {
  }
}
