package retrofit;

import com.squareup.okhttp.ResponseBody;

import java.lang.reflect.Type;

/**
 * An interface for preprocess every outgoing or incoming request to or from a server.
 *
 * This can be used for example to encode every {@link retrofit.http.Body} object into the same
 * wrapper object without the need of changing of the RestInterface's parameter.
 * Also you can use it backwards: preprocess every response from the server before
 * the interface call returns to you.
 */
public interface DataPreprocessor {

  /**
   * This method is called when a response arrives from the server.
   *
   * It is your duty to unwrap the data and return an Object with the given type.
   * For example, every query returns SSL encoded data and you get an encoded key and encoded data.
   * Then this method can decode the encoded data and should return the decoded object.
   *
   * @param responseBody  The response body from the server.
   * @param type          Type of the Object that should be extracted and returned
   *                      from the responseBody.
   * @return An instance with the given type.
   */
  public Object unWrapResponse(ResponseBody responseBody, Type type);


  /**
   * This method is called just before sending a {@link retrofit.http.Body} type parameter to the
   * server.
   *
   * This method can be used to wrap every object of yours before sending it to server, without the
   * need of changing of the RestInterface's parameter.
   * Like {@link #unWrapResponse(com.squareup.okhttp.ResponseBody, java.lang.reflect.Type)},
   * it can be used to wrap all of your requests with the same object for example for SSL encoding.
   *
   * @param object  The Object that should be wrapped.
   * @return A new wrapper instance Object that will be actually sent to the server. This should
   * hold the object that you got in parameter.
   */
  public Object wrapRequest(Object object);

}
