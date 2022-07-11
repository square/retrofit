//package retrofit2;
//
//import okhttp3.*;
//import org.junit.Before;
//import org.junit.Test;
//
//public class RequestBuilderTest {
//
//    private RequestBuilder requestBuilderUnderTest;
//
//    @Before
//    public void setUp() {
//        requestBuilderUnderTest = new RequestBuilder("method", HttpUrl.parse("url"), "relativeUrl",
//                Headers.of("namesAndValues"), MediaType.get("string"), false, false, false);
//    }
//
//    @Test
//    public void testSetRelativeUrl() {
//        // Setup
//        // Run the test
//        requestBuilderUnderTest.setRelativeUrl("relativeUrl");
//
//        // Verify the results
//    }
//
//    @Test
//    public void testAddHeader() {
//        // Setup
//        // Run the test
//        requestBuilderUnderTest.addHeader("name", "value");
//
//        // Verify the results
//    }
//
//    @Test
//    public void testAddHeaders() {
//        // Setup
//        final Headers headers = Headers.of("namesAndValues");
//
//        // Run the test
//        requestBuilderUnderTest.addHeaders(headers);
//
//        // Verify the results
//    }
//
//    @Test
//    public void testAddPathParam() {
//        // Setup
//        // Run the test
//        requestBuilderUnderTest.addPathParam("name", "value", false);
//
//        // Verify the results
//    }
//
//    @Test
//    public void testAddQueryParam() {
//        // Setup
//        // Run the test
//        requestBuilderUnderTest.addQueryParam("name", "value", false);
//
//        // Verify the results
//    }
//
//    @Test
//    public void testAddFormField() {
//        // Setup
//        // Run the test
//        requestBuilderUnderTest.addFormField("name", "value", false);
//
//        // Verify the results
//    }
//
//    @Test
//    public void testAddPart1() {
//        // Setup
//        final Headers headers = Headers.of("namesAndValues");
//        final RequestBody body = RequestBody.create(MediaType.get("string"), "content");
//
//        // Run the test
//        requestBuilderUnderTest.addPart(headers, body);
//
//        // Verify the results
//    }
//
//    @Test
//    public void testAddPart2() {
//        // Setup
//        final MultipartBody.Part part = MultipartBody.Part.createFormData("name", "value");
//
//        // Run the test
//        requestBuilderUnderTest.addPart(part);
//
//        // Verify the results
//    }
//
//    @Test
//    public void testSetBody() {
//        // Setup
//        final RequestBody body = RequestBody.create(MediaType.get("string"), "content");
//
//        // Run the test
//        requestBuilderUnderTest.setBody(body);
//
//        // Verify the results
//    }
//
//    @Test
//    public void testAddTag() {
//        // Setup
//        final Object value = null;
//
//        // Run the test
//        requestBuilderUnderTest.addTag(Object.class, value);
//
//        // Verify the results
//    }
//
//    @Test
//    public void testGet() {
//        // Setup
//        // Run the test
//        final Request.Builder result = requestBuilderUnderTest.get();
//
//        // Verify the results
//    }
//}
