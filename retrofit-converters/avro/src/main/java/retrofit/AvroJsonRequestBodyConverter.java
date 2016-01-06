package retrofit;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

final class AvroJsonRequestBodyConverter<T extends GenericRecord> implements Converter<T,
		RequestBody> {

	private static final MediaType MEDIA_TYPE = MediaType.parse("avro/json");
	private final DatumWriter<T> writer;
	private final Schema schema;

	public AvroJsonRequestBodyConverter(Schema schema) {
		this.schema = schema;
		writer = new SpecificDatumWriter<>(schema);
	}

	@Override
	public RequestBody convert(T value) throws IOException {

		ByteArrayOutputStream out = null;
		try {
			out = new ByteArrayOutputStream();
			JsonEncoder encoder = EncoderFactory.get().jsonEncoder(schema, out);
			writer.write(value, encoder);
			encoder.flush();
			out.flush();
			return RequestBody.create(MEDIA_TYPE, out.toByteArray());
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
}
