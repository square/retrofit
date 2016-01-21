package retrofit;

import com.squareup.okhttp.ResponseBody;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.JsonDecoder;
import org.apache.avro.specific.SpecificDatumReader;

import java.io.IOException;
import java.io.InputStream;

final class AvroJsonResponseBodyConverter<T extends GenericRecord> implements
		Converter<ResponseBody, T>{

	private final SpecificDatumReader<T> reader;

	public AvroJsonResponseBodyConverter(Schema schema) {
		reader = new SpecificDatumReader<>(schema);
	}

	@Override
	public T convert(ResponseBody value) throws IOException {
		InputStream inputStream = value.byteStream();
		try {
			JsonDecoder decoder = DecoderFactory.get().jsonDecoder(reader.getSchema(), inputStream);
			return reader.read(null, decoder);
		} finally  {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}
}
