package retrofit;

/** Intercept every request before it is executed in order to add additional data. */
public interface RequestInterceptor {
  /** Called for every request. Add data using methods on the supplied {@link RequestFacade}. */
  void intercept(RequestFacade request);

  interface RequestFacade {
    /** Add a header to the request. This will not replace any existing headers. */
    void addHeader(String name, String value);

    /**
     * Add a path parameter replacement. This works exactly like a {@link retrofit.http.Path
     * &#64;Path}-annotated method argument.
     */
    void addPathParam(String name, String value);

    /**
     * Add a path parameter replacement without first URI encoding. This works exactly like a
     * {@link retrofit.http.EncodedPath &#64;EncodedPath}-annotated method argument.
     */
    void addEncodedPathParam(String name, String value);

    /**
     * Add an additional query parameter. This will not replace any existing query parameters.
     *
     * @param name The name of the query parameter
     * @param value The value of the query parameter. It will have {@link #toString()} called on
     *              it and that string will be added as the value for the name. If the value is an
     *              array or an {@link Iterable} the parameter will be added multiple times, one for
     *              each value
     */
    void addQueryParam(String name, Object value);

    /**
     * Add an additional query parameter without first URI encoding. This will not replace any
     * existing query parameters.
     *
     * @param name The name of the query parameter
     * @param value The value of the query parameter. It will have {@link #toString()} called on
     *              it and that string will be added as the value for the name. If the value is an
     *              array or an {@link Iterable} the parameter will be added multiple times, one for
     *              each value
     */
    void addEncodedQueryParam(String name, Object value);
  }

  /** A {@link RequestInterceptor} which does no modification of requests. */
  RequestInterceptor NONE = new RequestInterceptor() {
    @Override public void intercept(RequestFacade request) {
      // Do nothing.
    }
  };
}
