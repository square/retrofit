package retrofit.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;

import org.junit.Test;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.HyphenStyle;
import org.simpleframework.xml.stream.Verbosity;

import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

public class SimpleXMLConverterTest {
	private static final String MIME_TYPE = "application/xml; charset=UTF-8";

	private final MyObject obj = new MyObject("hello world", 10);
	private final String objAsXML = String.format(
			"<my-object><message>%s</message><count>%d</count></my-object>",
			obj.getMessage(), obj.getCount());
	private final Converter converter = initConverter();

	private static Converter initConverter() {
		Format format = new Format(0, null, new HyphenStyle(), Verbosity.HIGH);
		Persister persister = new Persister(format);
		return new SimpleXMLConverter(persister);
	}

	@Test
	public void serialize() throws Exception {
		final TypedOutput typedOutput = converter.toBody(obj);
		assertThat(typedOutput.mimeType()).isEqualTo(MIME_TYPE);
		assertThat(asString(typedOutput)).isEqualTo(objAsXML);
	}

	@Test
	public void deserialize() throws Exception {
		final TypedInput input = new TypedByteArray(MIME_TYPE,
				objAsXML.getBytes());
		final MyObject result = (MyObject) converter.fromBody(input,
				MyObject.class);
		assertThat(result).isEqualTo(obj);
	}

	@Test(expected = ConversionException.class)
	public void deserializeWrongValue() throws Exception {
		final TypedInput input = new TypedByteArray(MIME_TYPE,
				"<myObject><foo/><bar/></myObject>".getBytes());
		converter.fromBody(input, MyObject.class);

	}

	@Test
	public void deserializeWrongClass() throws Exception {
		final TypedInput input = new TypedByteArray(MIME_TYPE,
				objAsXML.getBytes());
		Object result = converter.fromBody(input, String.class);
		assertThat(result).isNull();
	}

	private String asString(TypedOutput typedOutput) throws Exception {
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		typedOutput.writeTo(bytes);
		return new String(bytes.toByteArray());
	}

	@Default(value = DefaultType.FIELD)
	static class MyObject {
		@Element
		private String message;
		@Element
		private int count;

		public MyObject() {
		}

		public MyObject(String message, int count) {
			this.message = message;
			this.count = count;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public int getCount() {
			return count;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + count;
			result = prime * result
					+ ((message == null) ? 0 : message.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			MyObject other = (MyObject) obj;
			if (count != other.count) {
				return false;
			}
			if (message == null) {
				if (other.message != null) {
					return false;
				}
			} else if (!message.equals(other.message)) {
				return false;
			}
			return true;
		}
	}
}
