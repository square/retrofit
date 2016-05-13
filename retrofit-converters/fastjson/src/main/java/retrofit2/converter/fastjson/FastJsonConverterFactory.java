package retrofit2.converter.fastjson;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ParseProcess;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


/**
 * A {@linkplain Converter.Factory converter} which uses FastJson for JSON.
 * <p>
 * Because FastJson is so flexible in the types it supports, this converter assumes that it can handle
 * all types. If you are mixing JSON serialization with something else (such as protocol buffers),
 * you must {@linkplain Retrofit.Builder#addConverterFactory(Converter.Factory) add this instance}
 * last to allow the other converters a chance to see their types.
 */
public class FastJsonConverterFactory extends Converter.Factory {

    private ParserConfig mParserConfig = ParserConfig.getGlobalInstance();
    private int mFeatureValues = JSON.DEFAULT_PARSER_FEATURE;
    private Feature[] mFeatures;

    private SerializeConfig mSerializeConfig;
    private SerializerFeature[] mSerializerFeatures;

    /**
     * Create an default instance for conversion. Encoding to JSON and
     * decoding from JSON (when no charset is specified by a header) will use UTF-8.
     */
    public static FastJsonConverterFactory create() {
        return new FastJsonConverterFactory();
    }

    private FastJsonConverterFactory() {
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        return new FastJsonResponseBodyConverter<>(type, mParserConfig, mFeatureValues, mFeatures);
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        return new FastJsonRequestBodyConverter<>(mSerializeConfig, mSerializerFeatures);
    }

    public ParserConfig getParserConfig() {
        return mParserConfig;
    }

    public FastJsonConverterFactory setParserConfig(ParserConfig config) {
        this.mParserConfig = config;
        return this;
    }

    public int getParserFeatureValues() {
        return mFeatureValues;
    }

    public FastJsonConverterFactory setParserFeatureValues(int featureValues) {
        this.mFeatureValues = featureValues;
        return this;
    }

    public Feature[] getParserFeatures() {
        return mFeatures;
    }

    public FastJsonConverterFactory setParserFeatures(Feature[] features) {
        this.mFeatures = features;
        return this;
    }

    public SerializeConfig getSerializeConfig() {
        return mSerializeConfig;
    }

    public FastJsonConverterFactory setSerializeConfig(SerializeConfig serializeConfig) {
        this.mSerializeConfig = serializeConfig;
        return this;
    }

    public SerializerFeature[] getSerializerFeatures() {
        return mSerializerFeatures;
    }

    public FastJsonConverterFactory setSerializerFeatures(SerializerFeature[] serializerFeatures) {
        this.mSerializerFeatures = serializerFeatures;
        return this;
    }
}
