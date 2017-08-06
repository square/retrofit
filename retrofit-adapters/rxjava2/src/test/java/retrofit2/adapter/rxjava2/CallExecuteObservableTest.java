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

import org.junit.Before;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.mock.Calls;

import static org.assertj.core.api.Assertions.assertThat;

public class CallExecuteObservableTest {

  private Call<String> executedCall;
  private Call<String> nonExecutedCall;


  @Before
  public void setUp() throws Exception {
    executedCall = Calls.response("executed");
    executedCall.execute();
    nonExecutedCall = Calls.response("nonExecuted");

  }


  @Test
  public void dontcloneCallIfNotExecuted() throws Exception {
    CallExecuteObservable<String> callExecuteObservable = new CallExecuteObservable<>(nonExecutedCall);
    Call<String> nonClonedCalled = callExecuteObservable.cloneCallIfAlreadyExecuted(nonExecutedCall);
    assertThat(nonClonedCalled.isExecuted()).isFalse();
    assertThat(nonExecutedCall).isEqualTo(nonClonedCalled);
  }

  @Test
  public void cloneCallIfAlreadyExecuted() throws Exception {
    CallExecuteObservable<String> callExecuteObservable = new CallExecuteObservable<>(executedCall);
    Call<String> clonedCall = callExecuteObservable.cloneCallIfAlreadyExecuted(executedCall);
    assertThat(clonedCall.isExecuted()).isFalse();
    assertThat(executedCall).isNotEqualTo(clonedCall);
  }
}