package retrofit;

import com.bluelinelabs.logansquare.JsonMapper;
import com.bluelinelabs.logansquare.LoganSquare;

import java.lang.reflect.Type;

/**
 * A {@linkplain Converter.Factory converter} which uses LoganSquare as its conversion backbone.
 *
 * @author Marcel Schnelle (aurae)
 * @see <a>https://github.com/bluelinelabs/LoganSquare</a>
 */
public final class LoganSquareConverterFactory implements Converter.Factory {

	@Override public Converter<?> get(Type type) {
		// Obtain the JsonMapper for the type and serve a Converter
		JsonMapper<?> mapper = LoganSquare.mapperFor((Class<?>) type);
		return new LoganSquareConverter<>(mapper);
	}
}
