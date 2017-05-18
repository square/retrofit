package harsh.dalwadi.retrofitretryhelper;

import retrofit2.Call;
import retrofit2.Response;

/**
 * we can use this class directly to access the network calls when you
 * are facing problem with different error code then success code and in that
 * case if you you want to set autometically retry the network call.
 */
public class RetryHelper {
    private static final int DEFAULT_RETRIES = 1;
    private static int SUCCESS_CODE = 200;

    /**
     * Method with custom retry counts
     */
    public static <T> void enqueueRetry(Call<T> call, final int retryCount, final CustomCallback<T> callback) {
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
        return response.code() == SUCCESS_CODE;
    }

    /**
     * getter method to get the success code
     *
     * @return Success code
     */
    public static int getSuccessCode() {
        return SUCCESS_CODE;
    }

    /**
     * setter method to set custom success code
     *
     * @param successCode
     */
    public static void setSuccessCode(int successCode) {
        RetryHelper.SUCCESS_CODE = successCode;
    }

}
