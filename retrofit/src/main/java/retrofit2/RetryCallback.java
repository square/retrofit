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
package retrofit2;

class RetryCallback<T> implements CustomCallback<T> {
  private int totalRetries = 0;
  private static final String TAG = RetryCallback.class.getSimpleName();
  private final Call<T> call;
  private int retryCount = 0;

  RetryCallback(Call<T> call, int totalRetries) {
    this.call = call;
    this.totalRetries = totalRetries;
  }

  @Override
  public void onResponse(Call<T> call, Response<T> response) {
    if (!RetryHelper.isCallSuccess(response)) {
      if (retryCount++ < totalRetries) {
        retry();
      } else {
        onFinalFail(response.code(), call, response);
      }
    } else {
      onFinalResponse(call, response);
    }
  }


  @Override
  public void onFailure(Call<T> call, Throwable t) {
    onFinalFailure(call, t);
  }

  @Override
  public void onFailResponse(int errorCode, Call<T> call, Response<T> response) {
    onFinalFail(response.code(), call, response);
  }

  public void onFinalResponse(Call<T> call, Response<T> response) {

  }

  public void onFinalFailure(Call<T> call, Throwable t) {

  }

  public void onFinalFail(int errorCode, Call<T> call, Response<T> response) {

  }

  private void retry() {
    call.clone().enqueue(this);
  }
}
