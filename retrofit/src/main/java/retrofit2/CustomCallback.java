package retrofit2;

public interface CustomCallback<T> extends Callback<T> {
  /**
   * Invoked when a network fluctuation occurred talking to the server
   * on processing the response.
   *
   * @param errorCode - when after retrying the call still fails with errocode
   */
  void onFailResponse(int errorCode, Call<T> call, Response<T> response);
}
