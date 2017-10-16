package retrofit2.converter.fruit;

import me.ghui.fruit.Fruit;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Created by ghui on 07/04/2017.
 */

public class FruitConverterFactory extends Converter.Factory {

    private Fruit mPicker;

    public static FruitConverterFactory create(Fruit fruit) {
        return new FruitConverterFactory(fruit);
    }

    public static FruitConverterFactory create() {
        return new FruitConverterFactory(new Fruit());
    }


    private FruitConverterFactory(Fruit fruit) {
        mPicker = fruit;
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(
            Type type, Annotation[] annotations, Retrofit retrofit) {
        return new FruitResponseBodyConverter<>(mPicker, type);
    }

}
