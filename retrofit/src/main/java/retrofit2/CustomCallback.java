package retrofit2;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public interface CustomCallback<T> extends Callback<T> {
  /**
   * Invoked when a network fluctuation occurred talking to the server
   * on processing the response.
   *
   * @param errorCode - when after retrying the call still fails with errocode
   */
  void onFailResponse(int errorCode, Call<T> call, Response<T> response);
}
