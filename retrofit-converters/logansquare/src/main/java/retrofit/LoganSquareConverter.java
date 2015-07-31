package retrofit;

import com.bluelinelabs.logansquare.JsonMapper;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;

/**
 * A {@linkplain Converter converter} which uses LoganSquare as its conversion backbone.
 *
 * @author Marcel Schnelle (aurae)
 * @see <a>https://github.com/bluelinelabs/LoganSquare</a>
 */
final class LoganSquareConverter<T> implements Converter<T> {

	/** Media Type for serialized request bodies */
	private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");

	/** LoganSquare JSON mapper for parsing & serialization */
	private final JsonMapper<T> mapper;

	/**
	 * Constructor
	 *
	 * @param mapper LoganSquare JSON mapper used to parse & serialize objects
	 */
	public LoganSquareConverter(JsonMapper<T> mapper) {
		this.mapper = mapper;
	}

	@Override public T fromBody(ResponseBody body) throws IOException {
		return mapper.parse(body.byteStream());
	}

	@Override public RequestBody toBody(T value) {
		String serializedValue;
		try {
			serializedValue = mapper.serialize(value);
		} catch (IOException ex) {
			throw new AssertionError(ex);
		}
		return RequestBody.create(MEDIA_TYPE, serializedValue);
	}
}
