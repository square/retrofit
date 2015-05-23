package retrofit.converter;


import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;

import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.Test;

import java.io.IOException;

import okio.Buffer;

import static org.assertj.core.api.Assertions.assertThat;

public class LoganSquareConverterTest {

    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");
    private static final MyObject OBJECT = new MyObject("hello world", 10);
    private final String JSON = "{\"message\":\"hello world\",\"count\":10}";
    private LoganSquareConverter converter = new LoganSquareConverter();

    @Test
    public void serialize() throws Exception {
        RequestBody body = converter.toBody(OBJECT, MyObject.class);
        //assertThat(body.contentType()).isEqualTo(MEDIA_TYPE);
        //assertBody(body).isEqualTo(JSON);
    }

    @Test
    public void deserialize() throws Exception {
    }

    private static AbstractCharSequenceAssert<?, String> assertBody(RequestBody body) throws IOException {
        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        return assertThat(buffer.readUtf8());
    }
}