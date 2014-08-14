package retrofit.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An HTTP Response
 */
abstract class AbstractResponse<T> {
    protected final String url;
    protected final int status;
    protected final String reason;
    protected final List<Header> headers;
    protected final T body;

    public AbstractResponse(String url, int status, String reason, List<Header> headers, T body) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (status < 200) {
            throw new IllegalArgumentException("Invalid status code: " + status);
        }
        if (reason == null) {
            throw new IllegalArgumentException("reason == null");
        }
        if (headers == null) {
            throw new IllegalArgumentException("headers == null");
        }

        this.url = url;
        this.status = status;
        this.reason = reason;
        this.headers = Collections.unmodifiableList(new ArrayList<Header>(headers));
        this.body = body;
    }

    /** Request URL. */
    public String getUrl() {
        return url;
    }

    /** Status line code. */
    public int getStatus() {
        return status;
    }

    /** Status line reason phrase. */
    public String getReason() {
        return reason;
    }

    /** An unmodifiable collection of headers. */
    public List<Header> getHeaders() {
        return headers;
    }

    /** Response body.  May be {@code null}. */
    public T getBody() {
        return body;
    }
}
