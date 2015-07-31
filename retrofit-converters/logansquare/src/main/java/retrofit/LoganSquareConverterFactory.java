package retrofit;

import com.bluelinelabs.logansquare.JsonMapper;
import com.bluelinelabs.logansquare.LoganSquare;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;


/**
 * A {@linkplain Converter.Factory converter} which uses LoganSquare for JSON.
 *
 * @see <a>https://github.com/bluelinelabs/LoganSquare</a>
 */
public final class LoganSquareConverterFactory implements Converter.Factory {
	/**
	 * Create an instance. Encoding to JSON and decoding from JSON will use UTF-8.
	 */
	public static LoganSquareConverterFactory create() {
		return new LoganSquareConverterFactory();
	}


	private LoganSquareConverterFactory() {
	}


	private JsonMapper<?> mapperFor(Type type) {
		return LoganSquare.mapperFor((Class<?>) type);
	}


	@Override public Converter<?> get(Type type) {
		if (type instanceof Class) {
			// Return a plain Object converter
			return new LoganSquareObjectConverter<>(mapperFor(type));

		} else if (type instanceof ParameterizedType) {
			// Return a List or Map converter
			ParameterizedType parameterizedType = (ParameterizedType) type;
			Type[] typeArguments = parameterizedType.getActualTypeArguments();
			Type firstType = typeArguments[0];

			// Check for Map arguments
			if (parameterizedType.getRawType() == Map.class) {
				Type secondType = typeArguments[1];

				// Perform validity checks on the type arguments, since LoganSquare works only on String keys
				if (firstType != String.class) throw new RuntimeException("LoganSquareConverter can't convert Map keys of type '" + firstType + '\'');
				if (!(secondType instanceof Class)) throw new RuntimeException("LoganSquareConverter can't convert Map values of type '" + secondType + '\'');

				// Return a Map converter
				return new LoganSquareMapConverter<>(mapperFor(secondType));

			} else {
				// Otherwise, access the ParametrizedType's only type argument and perform validity checks on it
				if (!(firstType instanceof Class)) throw new RuntimeException("LoganSquareConverter can't convert List items of type '" + firstType + '\'');

				// Return a List converter
				return new LoganSquareListConverter<>(mapperFor(firstType));
			}
		}

		throw new RuntimeException("LoganSquareConverter encountered non-convertable type '" + type + '\'');
	}
}
