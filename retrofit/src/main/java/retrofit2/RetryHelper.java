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

/**
 * we can use this class directly to access the network calls when you
 * are facing problem with different error code then success code and in that
 * case if you you want to set autometically retry the network call.
 */
public class RetryHelper {
  private static final int DEFAULT_RETRIES = 1;
  private static int SUCCESS = 200;

  /**
   * Method with custom retry counts
   */
  public static <T> void enqueueRetry(Call<T> call, final int retryCount,
                                      final CustomCallback<T> callback) {

    call.enqueue(new RetryCallback<T>(call, retryCount) {
      @Override
      public void onFinalFail(int errorCode, Call<T> call, Response<T> response) {
        callback.onFailResponse(errorCode, call, response);
      }

      @Override
      public void onFinalResponse(Call<T> call, Response<T> response) {
        callback.onResponse(call, response);
      }

      @Override
      public void onFinalFailure(Call<T> call, Throwable t) {
        callback.onFailure(call, t);
      }
    });
  }

  /**
   * if you are go with default retry counts
   */
  public static <T> void enqueueRetry(Call<T> call, final CustomCallback<T> callback) {
    enqueueRetry(call, DEFAULT_RETRIES, callback);
  }

  static boolean isCallSuccess(Response response) {
    return response.code() == SUCCESS;
  }

  /**
   * getter method to get the success code
   *
   * @return Success code
   */
  public static int getSuccessCode() {
    return SUCCESS;
  }

  /**
   * setter method to set custom success code
   *
   * @param successCode
   */
  public static void setSuccessCode(int successCode) {
    RetryHelper.SUCCESS = successCode;
  }

}
