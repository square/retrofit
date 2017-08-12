/*
 * Copyright (C) 2017 Square, Inc.
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
package retrofit2.adapter.rxjava2;

import io.reactivex.observers.TestObserver;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.mock.Calls;

import static org.assertj.core.api.Assertions.assertThat;

public class CallExecuteObservableTest {
  private static final String RESPONSE = "RESPONSE";

  @Test
  public void callExcutedObservableCanBeSubscribedToMultipleTimes() throws Exception {
    Call<String> call = Calls.response(RESPONSE);
    CallExecuteObservable<String> callExecuteObservable = new CallExecuteObservable<>(call);
    assertThat(call.isExecuted()).isFalse();

    //subscribing to a CallExecuteObservable should execute the call
    subscribeAndAssertSuccess(callExecuteObservable);
    assertThat(call.isExecuted()).isTrue();

    //subscribing second time should succeed even when call already executed
    subscribeAndAssertSuccess(callExecuteObservable);
  }

  private void subscribeAndAssertSuccess(CallExecuteObservable<String> callExecuteObservable) {
    TestObserver<Response<String>> testSubscriber = new TestObserver<>();
    callExecuteObservable.subscribeActual(testSubscriber);
    testSubscriber.assertNoErrors()
        .assertComplete()
        .assertValueCount(1);
  }
}
