// Copyright 2011 Square, Inc.
package retrofit.http;

/**
 * Use this annotation on a service method param when you want to directly control the request body
 * of a POST/PUT request (instead of sending in as request parameters or form-style request
 * body).  If the value of the parameter implements TypedBytes, the request body will be written
 * exactly as specified by the TypedBytes.writeTo object.  If it doesn't implement TypedBytes, the
 * object will be serialized into JSON and the result will be set directly as the request body.
 *
 * @author Eric Denman (edenman@squareup.com)
 */
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(java.lang.annotation.ElementType.PARAMETER)
public @interface SingleEntity {
}
