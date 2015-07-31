package retrofit;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.bluelinelabs.logansquare.typeconverters.TypeConverter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit.http.Body;
import retrofit.http.POST;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@linkplain LoganSquareConverter}.
 *
 * @author Marcel Schnelle (aurae)
 * @see <a>https://github.com/bluelinelabs/LoganSquare</a>
 */
public class LoganSquareConverterTest {

	@JsonObject
	static class Implementation {

		@JsonField
		String name;

		@JsonField(typeConverter = CustomTypeConverter.class)
		CustomType customType;

		@JsonField(name = "list")
		List<String> values;

		String notSerialized;

		public Implementation() {

		}

		public Implementation(String name, String notSerialized, CustomType customType, List<String> values) {
			this();
			this.name = name;
			this.notSerialized = notSerialized;
			this.customType = customType;
			this.values = values;
		}

		public String getName() {
			return name;
		}

		public String getNotSerialized() {
			return notSerialized;
		}

		public CustomType getCustomType() {
			return customType;
		}

		public List<String> getValues() {
			return values;
		}
	}

	enum CustomType {
		VAL_1(1),
		VAL_2(2);

		private int val;

		CustomType(int val) {
			this.val = val;
		}

		static CustomType fromInt(int val) {
			for (CustomType customType : values()) {
				if (customType.val == val) return customType;
			}
			throw new AssertionError("custom type with value " + val + " not found.");
		}
	}

	static class CustomTypeConverter implements TypeConverter<CustomType> {

		@Override public CustomType parse(JsonParser jsonParser) throws IOException {
			return CustomType.fromInt(jsonParser.getIntValue());
		}

		@Override public void serialize(CustomType object, String fieldName, boolean writeFieldNameForObject, JsonGenerator jsonGenerator) throws IOException {
			jsonGenerator.writeFieldName(fieldName);
			jsonGenerator.writeNumber(object.val);
		}
	}

	interface Service {
		@POST("/") Call<Implementation> callImplementation(@Body Implementation body);
	}

	@Rule public final MockWebServer mockWebServer = new MockWebServer();

	private Service service;

	@Before public void setUp() {
		service = new Retrofit.Builder()
				.baseUrl(mockWebServer.url("/"))
				.converterFactory(new LoganSquareConverterFactory())
				.build()
				.create(Service.class);
	}

	@Test public void testImplementation() throws IOException, InterruptedException {
		// Enqueue a mock response
		mockWebServer.enqueue(new MockResponse().setBody("{\"customType\":2,\"name\":\"LOGAN SQUARE IS COOL\",\"list\":[\"value1\",\"value2\"]}"));

		// Setup the mock object
		List<String> values = new ArrayList<>();
		values.add("value1");
		values.add("value2");
		Implementation requestBody = new Implementation("LOGAN SQUARE IS COOL", "Not serialized", CustomType.VAL_2, values);

		// Call the API and execute it
		Call<Implementation> call = service.callImplementation(requestBody);
		Response<Implementation> response = call.execute();
		Implementation responseBody = response.body();

		// Assert that conversions worked
		// 1) Standard field declaration
		assertThat(responseBody.getName()).isEqualTo("LOGAN SQUARE IS COOL");
		// 2) Named field declaration & List serialization
		assertThat(responseBody.getValues()).containsExactly("value1", "value2");
		// 3) Custom Type adapter serialization
		assertThat(responseBody.getCustomType()).isEqualTo(CustomType.VAL_2);
		// 4) Excluded field
		assertThat(responseBody.getNotSerialized()).isNull();

		// Check request body and the received header
		RecordedRequest request = mockWebServer.takeRequest();
		assertThat(request.getBody().readUtf8()).isEqualTo("{\"customType\":2,\"name\":\"LOGAN SQUARE IS COOL\",\"list\":[\"value1\",\"value2\"]}");
		assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
	}
}
