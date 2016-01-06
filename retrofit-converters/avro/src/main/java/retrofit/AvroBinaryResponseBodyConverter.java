package retrofit;

import com.squareup.okhttp.ResponseBody;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;

import java.io.IOException;
import java.io.InputStream;

final class AvroBinaryResponseBodyConverter<T extends GenericRecord> implements
		Converter<ResponseBody, T>{

	private final SpecificDatumReader<T> reader;
	private final BinaryDecoder decoder = DecoderFactory.get().binaryDecoder((InputStream)null,
			null);

	public AvroBinaryResponseBodyConverter(Schema schema) {
		reader = new SpecificDatumReader<>(schema);
	}

	@Override
	public T convert(ResponseBody value) throws IOException {
		InputStream inputStream = value.byteStream();
		try {
			return reader.read(null, DecoderFactory.get().binaryDecoder(inputStream, decoder));
		} finally  {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}
}
