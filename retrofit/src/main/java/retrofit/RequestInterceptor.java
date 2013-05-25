package retrofit;

/** Intercept every request before it is executed in order to add additional data. */
public interface RequestInterceptor {
  /** Called for every request. Add data using methods on the supplied {@link RequestFacade}. */
  void intercept(RequestFacade request);

  interface RequestFacade {
    /** Add a header to the request. This will not replace any existing headers. */
    void addHeader(String name, String value);

    /**
     * Add a path parameter replacement. This works exactly like a {@link retrofit.http.Part
     * &#64;Part}-annotated method argument.
     */
    void addPathParam(String name, String value);
  }

  /** A {@link RequestInterceptor} which does no modification of requests. */
  RequestInterceptor NONE = new RequestInterceptor() {
    @Override public void intercept(RequestFacade request) {
      // Do nothing.
    }
  };
}
