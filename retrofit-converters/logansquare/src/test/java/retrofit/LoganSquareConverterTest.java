package retrofit;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.bluelinelabs.logansquare.typeconverters.TypeConverter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.http.Body;
import retrofit.http.POST;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@linkplain LoganSquareObjectConverter}.
 *
 * @author marcel
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
		@POST("/") Call<List<Implementation>> callList(@Body List<Implementation> body);
		@POST("/") Call<Implementation> callListWrongType(@Body List<List<Implementation>> body);
		@POST("/") Call<Map<String, Implementation>> callMap(@Body Map<String, Implementation> body);
		@POST("/") Call<Map<Integer, Implementation>> callMapWrongKey(@Body Map<Integer, Implementation> body);
		@POST("/") Call<Map<String, List<Implementation>>> callMapWrongValue(@Body Map<String, List<Implementation>> body);
		@POST("/") Call<Implementation[]> callArray(@Body Implementation[] body);
	}

	@Rule public final MockWebServer mockWebServer = new MockWebServer();

	private Service service;

	@Before public void setUp() {
		service = new Retrofit.Builder()
				.baseUrl(mockWebServer.url("/"))
				.converterFactory(LoganSquareConverterFactory.create())
				.build()
				.create(Service.class);
	}

	@Test public void testObject() throws IOException, InterruptedException {
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

	@Test public void testList() throws IOException, InterruptedException {
		// Enqueue a mock response
		mockWebServer.enqueue(new MockResponse().setBody("[{\"customType\":2,\"name\":\"LOGAN SQUARE IS COOL\",\"list\":[\"value1\",\"value2\"]},{\"customType\":1,\"name\":\"LOGAN SQUARE IS COOL2\",\"list\":[\"value1\",\"value2\"]}]"));

		// Setup the mock object
		List<String> values = new ArrayList<>();
		values.add("value1");
		values.add("value2");
		List<Implementation> requestBody = new ArrayList<>();
		requestBody.add(new Implementation("LOGAN SQUARE IS COOL", "Not serialized", CustomType.VAL_2, values));
		requestBody.add(new Implementation("LOGAN SQUARE IS COOL2", "Not serialized", CustomType.VAL_1, values));

		// Call the API and execute it
		Call<List<Implementation>> call = service.callList(requestBody);
		Response<List<Implementation>> response = call.execute();
		List<Implementation> responseBody = response.body();

		// Assert that conversions worked
		// Number of objects
		assertThat(responseBody).hasSize(2);
		// Member values of first object
		Implementation o1 = responseBody.get(0);
		assertThat(o1.getName()).isEqualTo("LOGAN SQUARE IS COOL");
		assertThat(o1.getValues()).containsExactly("value1", "value2");
		assertThat(o1.getCustomType()).isEqualTo(CustomType.VAL_2);
		assertThat(o1.getNotSerialized()).isNull();
		// Member values of second object
		Implementation o2 = responseBody.get(1);
		assertThat(o2.getName()).isEqualTo("LOGAN SQUARE IS COOL2");
		assertThat(o2.getValues()).containsExactly("value1", "value2");
		assertThat(o2.getCustomType()).isEqualTo(CustomType.VAL_1);
		assertThat(o2.getNotSerialized()).isNull();

		// Check request body and the received header
		RecordedRequest request = mockWebServer.takeRequest();
		assertThat(request.getBody().readUtf8()).isEqualTo("[{\"customType\":2,\"name\":\"LOGAN SQUARE IS COOL\",\"list\":[\"value1\",\"value2\"]},{\"customType\":1,\"name\":\"LOGAN SQUARE IS COOL2\",\"list\":[\"value1\",\"value2\"]}]");
		assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
	}

	@Test public void testListWrongType() throws IOException {
		// Setup the mock object with an incompatible type argument
		List<List<Implementation>> body = new ArrayList<>();

		// Setup the API call and fire it
		try {
			service.callListWrongType(body);
			Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
		} catch (RuntimeException ex) {
		}
	}

	@Test public void testMap() throws IOException, InterruptedException {
		// Enqueue a mock response
		mockWebServer.enqueue(new MockResponse().setBody("{\"item1\":{\"customType\":2,\"name\":\"LOGAN SQUARE IS COOL\",\"list\":[\"value1\",\"value2\"]},\"item2\":{\"customType\":1,\"name\":\"LOGAN SQUARE IS COOL2\",\"list\":[\"value1\",\"value2\"]}}"));

		// Setup the mock object
		List<String> values = new ArrayList<>();
		values.add("value1");
		values.add("value2");
		Map<String, Implementation> requestBody = new HashMap<>();
		requestBody.put("item1", new Implementation("LOGAN SQUARE IS COOL", "Not serialized", CustomType.VAL_2, values));
		requestBody.put("item2", new Implementation("LOGAN SQUARE IS COOL2", "Not serialized", CustomType.VAL_1, values));

		// Call the API and execute it
		Call<Map<String, Implementation>> call = service.callMap(requestBody);
		Response<Map<String, Implementation>> response = call.execute();
		Map<String, Implementation> responseBody = response.body();

		// Assert that conversions worked
		// Number of objects
		assertThat(responseBody).hasSize(2);
		// Member values of first object
		Implementation o1 = responseBody.get("item1");
		assertThat(o1.getName()).isEqualTo("LOGAN SQUARE IS COOL");
		assertThat(o1.getValues()).containsExactly("value1", "value2");
		assertThat(o1.getCustomType()).isEqualTo(CustomType.VAL_2);
		assertThat(o1.getNotSerialized()).isNull();
		// Member values of second object
		Implementation o2 = responseBody.get("item2");
		assertThat(o2.getName()).isEqualTo("LOGAN SQUARE IS COOL2");
		assertThat(o2.getValues()).containsExactly("value1", "value2");
		assertThat(o2.getCustomType()).isEqualTo(CustomType.VAL_1);
		assertThat(o2.getNotSerialized()).isNull();

		// Check request body and the received header
		RecordedRequest request = mockWebServer.takeRequest();
		assertThat(request.getBody().readUtf8()).isEqualTo("{\"item2\":{\"customType\":1,\"name\":\"LOGAN SQUARE IS COOL2\",\"list\":[\"value1\",\"value2\"]},\"item1\":{\"customType\":2,\"name\":\"LOGAN SQUARE IS COOL\",\"list\":[\"value1\",\"value2\"]}}");
		assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
	}

	@Test public void testMapWrongKeyType() throws IOException {
		// Setup the mock object with an incompatible type argument
		Map<Integer, Implementation> body = new HashMap<>();

		// Setup the API call and fire it
		try {
			service.callMapWrongKey(body);
			Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
		} catch (RuntimeException ex) {
		}
	}

	@Test public void testMapWrongValueType() throws IOException {
		// Setup the mock object with an incompatible type argument
		Map<String, List<Implementation>> body = new HashMap<>();

		// Setup the API call and fire it
		try {
			service.callMapWrongValue(body);
			Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
		} catch (RuntimeException ex) {
		}
	}

	@Test public void testFailWhenArray() throws IOException {
		// Setup the mock object with an incompatible type argument
		Implementation[] body = new Implementation[0];

		// Setup the API call and fire it
		try {
			service.callArray(body);
			Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
		} catch (RuntimeException ex) {
		}
	}
}
