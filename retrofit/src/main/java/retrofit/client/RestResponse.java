package retrofit.client;

import java.util.List;

/**
 * A deserialized HTTP response.  Don't use with {@link retrofit.http.Streaming @Streaming}
 *
 * @author mattrasband on 8/14/14.
 */
public class RestResponse<T> extends AbstractResponse<T> {
    public RestResponse(String url, int status, String reason, List<Header> headers, T body) {
        super(url, status, reason, headers, body);
    }
}
