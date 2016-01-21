package retrofit;

import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

/**
 * A {@linkplain Converter.Factory} which uses Avro for reading and writing entities from
 * binary
 */
public class AvroBinaryConverterFactory extends Converter.Factory {

	public static AvroBinaryConverterFactory create() {
		return new AvroBinaryConverterFactory();
	}

	@Override
	public Converter<ResponseBody, ?> fromResponseBody(Type type, Annotation[] annotations) {
		if (!(type instanceof Class<?>)) {
			return null;
		}
		Class<?> c = (Class<?>) type;
		if (!GenericRecord.class.isAssignableFrom(c)) {
			return null;
		}

		Schema schema;
		try {
			schema = AvroUtils.extractSchema(c);
		} catch (InvocationTargetException | NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalArgumentException("Found an avro but unable to convert " + c.getName());
		}
		return new AvroBinaryResponseBodyConverter<>(schema);
	}

	@Override
	public Converter<?, RequestBody> toRequestBody(Type type, Annotation[] annotations) {
		if (!(type instanceof Class<?>)) {
			return null;
		}
		Class<?> c = (Class<?>) type;
		if (!GenericRecord.class.isAssignableFrom(c)) {
			return null;
		}
		Schema schema;
		try {
			schema = AvroUtils.extractSchema(c);
		} catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
			throw new IllegalArgumentException("Found an avro but anble to convert " + c.getName());
		}
		return new AvroBinaryRequestBodyConverter<>(schema);
	}
}
