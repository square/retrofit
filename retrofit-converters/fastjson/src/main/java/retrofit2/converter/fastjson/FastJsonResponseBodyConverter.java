package retrofit2.converter.fastjson;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ParseProcess;
import okhttp3.ResponseBody;
import retrofit2.Converter;

import java.io.IOException;
import java.lang.reflect.Type;

final class FastJsonResponseBodyConverter<T> implements Converter<ResponseBody, T> {

    private static final Feature[] EMPTY_SERIALIZER_FEATURES = new Feature[0];

    private Type mType;

    private ParserConfig mConfig;
    private int mFeatureValues;
    private Feature[] mFeatures;

    FastJsonResponseBodyConverter(Type type, ParserConfig config, int featureValues,
                                         Feature... features) {
        mType = type;
        mConfig = config;
        mFeatureValues = featureValues;
        mFeatures = features;
    }

    @Override
    public T convert(ResponseBody value) throws IOException {
        try {
            return JSON.parseObject(value.string(), mType, mConfig, mFeatureValues,
                    mFeatures != null ? mFeatures : EMPTY_SERIALIZER_FEATURES);
        } finally {
            value.close();
        }
    }
}
