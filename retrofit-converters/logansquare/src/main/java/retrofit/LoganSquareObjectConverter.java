package retrofit;

import com.bluelinelabs.logansquare.JsonMapper;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;

import static retrofit.LoganSquareConverterUtils.MEDIA_TYPE;


final class LoganSquareObjectConverter<T> implements Converter<T> {


	private final JsonMapper<T> mapper;


	public LoganSquareObjectConverter(JsonMapper<T> mapper) {
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
