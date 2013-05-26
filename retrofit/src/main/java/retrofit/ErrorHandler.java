package retrofit;


/**
 * A hook allowing clients to customize error exceptions for synchronous
 * requests.
 *
 * @author Sam Beran sberan@gmail.com
 */
public interface ErrorHandler {
  /**
   * Return a custom exception to be thrown for this RetrofitError instance.
   *
   * If the exception is checked, any returned exceptions must be declared to be
   * thrown on the interface method.
   *
   * @param cause the original RetrofitError exception
   * @return Throwable an exception which will be thrown from the client interface method
   */
  Throwable handleError(RetrofitError cause);

  ErrorHandler DEFAULT = new ErrorHandler() {
    @Override public Throwable handleError(RetrofitError cause) {
      return cause;
    }
  };
}
