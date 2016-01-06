package retrofit;

import org.apache.avro.Schema;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.specific.SpecificRecord;

import java.lang.reflect.InvocationTargetException;

public class AvroUtils {

	private AvroUtils() {
	}

	public static <T> Schema extractSchema(Class<T> klass)
			throws IllegalAccessException, InvocationTargetException, NoSuchFieldException {
		if (!(klass.isInstance(SpecificRecord.class))) {
			return ReflectData.get().getSchema(klass);
		}
		return (Schema) (klass.getDeclaredField("$SCHEMA")).get(null);
	}
}
