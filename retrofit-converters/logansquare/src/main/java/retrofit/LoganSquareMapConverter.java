package retrofit;

import com.bluelinelabs.logansquare.JsonMapper;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.util.Map;

import static retrofit.LoganSquareConverterUtils.MEDIA_TYPE;


final class LoganSquareMapConverter<T> implements Converter<Map<String, T>> {


	private final JsonMapper<T> mapper;


	public LoganSquareMapConverter(JsonMapper<T> mapper) {
		this.mapper = mapper;
	}


	@Override public Map<String, T> fromBody(ResponseBody body) throws IOException {
		return mapper.parseMap(body.byteStream());
	}


	@Override public RequestBody toBody(Map<String, T> value) {
		String serializedValue;
		try {
			serializedValue = mapper.serialize(value);
		} catch (IOException ex) {
			throw new AssertionError(ex);
		}
		return RequestBody.create(MEDIA_TYPE, serializedValue);
	}
}
