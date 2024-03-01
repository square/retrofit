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
package retrofit2;

import static android.os.Build.VERSION.SDK_INT;

import java.util.concurrent.Executor;
import javax.annotation.Nullable;

final class Platform {
  static final @Nullable Executor callbackExecutor;
  static final Reflection reflection;
  static final BuiltInFactories builtInFactories;

  static {
    switch (System.getProperty("java.vm.name")) {
      case "Dalvik":
        callbackExecutor = new AndroidMainExecutor();
        if (SDK_INT >= 24) {
          reflection = new Reflection.Android24();
          builtInFactories = new BuiltInFactories.Java8();
        } else {
          reflection = new Reflection();
          builtInFactories = new BuiltInFactories();
        }
        break;

      case "RoboVM":
        callbackExecutor = null;
        reflection = new Reflection();
        builtInFactories = new BuiltInFactories();
        break;

      default:
        callbackExecutor = null;
        reflection = new Reflection.Java8();
        builtInFactories = new BuiltInFactories.Java8();
        break;
    }
  }

  private Platform() {}
}
