package retrofit2;

import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Timeout;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OkHttpCallTest {

    @Mock
    private Converter<ResponseBody, Object> mockResponseConverter;

    private OkHttpCall<Object> okHttpCallUnderTest;

    @Before
    public void setUp() {
        okHttpCallUnderTest = new OkHttpCall<>(null, new Object[]{"args"}, null, mockResponseConverter);
    }

    @Test
    public void testClone() {
        // Setup
        // Run the test
        final OkHttpCall<Object> result = okHttpCallUnderTest.clone();

        // Verify the results
    }

    @Test
    public void testRequest() {
        // Setup
        // Run the test
        final Request result = okHttpCallUnderTest.request();

        // Verify the results
    }

    @Test
    public void testTimeout() {
        // Setup
        // Run the test
        final Timeout result = okHttpCallUnderTest.timeout();

        // Verify the results
    }

    @Test
    public void testEnqueue() throws Exception {
        // Setup
        final Callback<Object> mockCallback = mock(Callback.class);
        when(mockResponseConverter.convert(any(OkHttpCall.ExceptionCatchingResponseBody.class))).thenReturn(null);

        // Run the test
        okHttpCallUnderTest.enqueue(mockCallback);

        // Verify the results
    }

    @Test
    public void testEnqueue_ConverterReturnsNull() throws Exception {
        // Setup
        final Callback<Object> mockCallback = mock(Callback.class);
        when(mockResponseConverter.convert(any(OkHttpCall.ExceptionCatchingResponseBody.class))).thenReturn(null);

        // Run the test
        okHttpCallUnderTest.enqueue(mockCallback);

        // Verify the results
    }

    @Test
    public void testEnqueue_ConverterThrowsIOException() throws Exception {
        // Setup
        final Callback<Object> mockCallback = mock(Callback.class);
        when(mockResponseConverter.convert(any(OkHttpCall.ExceptionCatchingResponseBody.class))).thenThrow(
                IOException.class);

        // Run the test
        okHttpCallUnderTest.enqueue(mockCallback);

        // Verify the results
    }

    @Test
    public void testExecute() throws Exception {
        // Setup
        when(mockResponseConverter.convert(any(OkHttpCall.ExceptionCatchingResponseBody.class))).thenReturn(null);

        // Run the test
        final Response<Object> result = okHttpCallUnderTest.execute();

        // Verify the results
    }

    @Test
    public void testExecute_ConverterReturnsNull() throws Exception {
        // Setup
        when(mockResponseConverter.convert(any(OkHttpCall.ExceptionCatchingResponseBody.class))).thenReturn(null);

        // Run the test
        final Response<Object> result = okHttpCallUnderTest.execute();

        // Verify the results
    }

    @Test
    public void testExecute_ConverterThrowsIOException() throws Exception {
        // Setup
        when(mockResponseConverter.convert(any(OkHttpCall.ExceptionCatchingResponseBody.class))).thenThrow(
                IOException.class);

        // Run the test
        assertThrows(IOException.class, () -> okHttpCallUnderTest.execute());
    }

    @Test
    public void testParseResponse() throws Exception {
        // Setup
        final okhttp3.Response mockRawResponse = mock(okhttp3.Response.class);
        when(mockResponseConverter.convert(any(OkHttpCall.ExceptionCatchingResponseBody.class))).thenReturn(null);

        // Run the test
        final Response<Object> result = okHttpCallUnderTest.parseResponse(mockRawResponse);

        // Verify the results
    }

    @Test
    public void testParseResponse_ConverterReturnsNull() throws Exception {
        // Setup
        final okhttp3.Response mockRawResponse = mock(okhttp3.Response.class);
        when(mockResponseConverter.convert(any(OkHttpCall.ExceptionCatchingResponseBody.class))).thenReturn(null);

        // Run the test
        final Response<Object> result = okHttpCallUnderTest.parseResponse(mockRawResponse);

        // Verify the results
    }

    @Test
    public void testParseResponse_ConverterThrowsIOException() throws Exception {
        // Setup
        final okhttp3.Response mockRawResponse = mock(okhttp3.Response.class);
        when(mockResponseConverter.convert(any(OkHttpCall.ExceptionCatchingResponseBody.class))).thenThrow(
                IOException.class);

        // Run the test
        assertThrows(IOException.class, () -> okHttpCallUnderTest.parseResponse(mockRawResponse));
    }

    @Test
    public void testCancel() {
        // Setup
        // Run the test
        okHttpCallUnderTest.cancel();

        // Verify the results
    }
}
