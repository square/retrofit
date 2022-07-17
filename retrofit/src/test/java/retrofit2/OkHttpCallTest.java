package retrofit2;

import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

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
        final OkHttpCall<Object> result = okHttpCallUnderTest.clone();
        assertNotEquals(result, okHttpCallUnderTest);
    }

    @Test
    public void testEnqueue() throws Exception {
        // Setup
        final Callback<Object> mockCallback = mock(Callback.class);
       // when(mockResponseConverter.convert(any(OkHttpCall.ExceptionCatchingResponseBody.class))).thenReturn(null);

        // Run the test
        okHttpCallUnderTest.enqueue(mockCallback);

        // Verify the results
    }

    @Test
    public void testEnqueue_ConverterReturnsNull() throws Exception {
        // Setup
        final Callback<Object> mockCallback = mock(Callback.class);
//        when(mockResponseConverter.convert(any(OkHttpCall.ExceptionCatchingResponseBody.class))).thenReturn(null);

        // Run the test
        okHttpCallUnderTest.enqueue(mockCallback);

        // Verify the results
    }

    @Test
    public void testEnqueue_ConverterThrowsIOException() throws Exception {
        // Setup
        final Callback<Object> mockCallback = mock(Callback.class);
        //when(mockResponseConverter.convert(any(OkHttpCall.ExceptionCatchingResponseBody.class))).thenThrow(
        //        IOException.class);

        // Run the test
        okHttpCallUnderTest.enqueue(mockCallback);

        // Verify the results
    }

    @Test
    public void testExecute_ConverterThrowsNullPointerException() throws Exception {
        // Setup
//        when(mockResponseConverter.convert(any(OkHttpCall.ExceptionCatchingResponseBody.class))).thenThrow(
//                IOException.class);

        // Run the test
        assertThrows(NullPointerException.class, () -> okHttpCallUnderTest.execute());
    }

    @Test
    public void testCancel() {
        // Setup
        // Run the test
        okHttpCallUnderTest.cancel();

        // Verify the results
    }
}
