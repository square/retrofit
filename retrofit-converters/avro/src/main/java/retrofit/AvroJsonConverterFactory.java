package retrofit;

import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import retrofit.Converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

/**
 * A {@linkplain Converter.Factory} which uses Avro for reading and writing entities from
 * binary
 */
public class AvroJsonConverterFactory extends Converter.Factory {

	public static AvroJsonConverterFactory create() {
		return new AvroJsonConverterFactory();
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
		return new AvroJsonResponseBodyConverter<>(schema);
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
		} catch (InvocationTargetException | NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalArgumentException("Found an avro but unable to convert " + c.getName());
		}
		return new AvroJsonRequestBodyConverter<>(schema);
	}
}
