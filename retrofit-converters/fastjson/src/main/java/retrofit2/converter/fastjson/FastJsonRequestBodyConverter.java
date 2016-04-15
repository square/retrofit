package retrofit2.converter.fastjson;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Converter;

import java.io.IOException;

final class FastJsonRequestBodyConverter<T> implements Converter<T, RequestBody> {
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");
  private SerializeConfig serializeConfig;
  private SerializerFeature[] serializerFeatures;

  FastJsonRequestBodyConverter(SerializeConfig config, SerializerFeature... features) {
    serializeConfig = config;
    serializerFeatures = features;
  }

  @Override
  public RequestBody convert(T value) throws IOException {
    byte[] content;
    if (serializeConfig != null) {
      if (serializerFeatures != null) {
        content = JSON.toJSONBytes(value, serializeConfig, serializerFeatures);
      } else {
        content = JSON.toJSONBytes(value, serializeConfig);
      }
    } else {
      if (serializerFeatures != null) {
        content = JSON.toJSONBytes(value, serializerFeatures);
      } else {
        content = JSON.toJSONBytes(value);
      }
    }
    return RequestBody.create(MEDIA_TYPE, content);
  }
}
