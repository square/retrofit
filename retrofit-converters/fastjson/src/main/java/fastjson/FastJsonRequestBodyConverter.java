package fastjson;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONWriter;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import retrofit2.Converter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;


public final class FastJsonRequestBodyConverter<T> implements Converter<T, RequestBody> {
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");
    private static final Charset UTF8 = Charset.forName("UTF-8");

    @Override
    public RequestBody convert(T value) throws IOException {
        Buffer buffer = new Buffer();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(buffer.outputStream(), UTF8);
        JSONWriter jsonWriter = new JSONWriter(outputStreamWriter);
        jsonWriter.writeObject(value);
        jsonWriter.close();
        return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
    }
}
