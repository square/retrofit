package retrofit;

import com.bluelinelabs.logansquare.JsonMapper;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.util.List;

import static retrofit.LoganSquareConverterUtils.MEDIA_TYPE;


final class LoganSquareListConverter<T> implements Converter<List<T>> {


	private final JsonMapper<T> mapper;


	public LoganSquareListConverter(JsonMapper<T> mapper) {
		this.mapper = mapper;
	}


	@Override public List<T> fromBody(ResponseBody body) throws IOException {
		return mapper.parseList(body.byteStream());
	}


	@Override public RequestBody toBody(List<T> value) {
		String serializedValue;
		try {
			serializedValue = mapper.serialize(value);
		} catch (IOException ex) {
			throw new AssertionError(ex);
		}
		return RequestBody.create(MEDIA_TYPE, serializedValue);
	}
}
