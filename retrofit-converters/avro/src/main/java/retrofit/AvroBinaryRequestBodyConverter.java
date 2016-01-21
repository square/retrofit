package retrofit;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

final class AvroBinaryRequestBodyConverter<T extends GenericRecord> implements Converter<T,
		RequestBody> {

	private static final MediaType MEDIA_TYPE = MediaType.parse("avro/binary");

	private BinaryEncoder encoder;
	private final DatumWriter<T> writer;

	public AvroBinaryRequestBodyConverter(Schema schema) {
		writer = new SpecificDatumWriter<>(schema);
	}

	@Override
	public RequestBody convert(T value) throws IOException {

		ByteArrayOutputStream out = null;
		try {
			out = new ByteArrayOutputStream();

			encoder = EncoderFactory.get().binaryEncoder(out, encoder);

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
