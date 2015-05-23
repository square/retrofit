package retrofit.converter;

import com.bluelinelabs.logansquare.LoganSquare;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * A {@link Converter} which uses LoganSquare for serialization and deserialization of entities.
 */
public class LoganSquareConverter implements Converter {
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");

    public LoganSquareConverter() {}

    @Override
    public Object fromBody(ResponseBody body, Type type) throws IOException {
        InputStream is = body.byteStream();
        try {
            return LoganSquare.parse(is, (Class) type);
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public RequestBody toBody(Object object, Type type) {
        try {
            String json = LoganSquare.serialize(object);
            return RequestBody.create(MEDIA_TYPE, json);
        } catch (Exception e) {
            System.err.print(e);
            e.printStackTrace();
            return null;
        }
    }
}
