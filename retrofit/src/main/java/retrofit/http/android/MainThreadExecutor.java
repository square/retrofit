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
package retrofit.http.android;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

/** Executor that runs tasks on Android's main thread. */
public final class MainThreadExecutor implements Executor {
  private final Handler handler = new Handler(Looper.getMainLooper());

  @Override public void execute(Runnable r) {
    handler.post(r);
  }
}
