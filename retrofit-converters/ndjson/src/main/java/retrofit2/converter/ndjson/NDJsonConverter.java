/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package retrofit2.converter.ndjson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Converter for the new line delimited JSON format
 * <p>
 * <p>
 * Created by Cotuna Aurelian on 7/2/2016.
 */

public class NDJsonConverter extends Converter.Factory implements Converter<ResponseBody, BaseResponse> {

    /**
     * Creates a new instance of this converter
     */
    public static NDJsonConverter newInstance() {
        return new NDJsonConverter();
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        return new NDJsonConverter();
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations,
                    Retrofit retrofit) {
        return null;
    }

    @Override
    public Converter<?, String> stringConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        return super.stringConverter(type, annotations, retrofit);
    }


    @Override
    public BaseResponse convert(ResponseBody value) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(value.byteStream()));
        StringBuilder responseStringBuilder = new StringBuilder();

        String line = "";
        while ((line = bufferedReader.readLine()) != null) {
            responseStringBuilder.append(line);
        }

        //Close the buffers after we've done with them
        bufferedReader.close();
        value.close();

        if (responseStringBuilder.toString().trim().length() == 0) {
            //No Data received
            return null;
        }
        return new NDJsonResponse(responseStringBuilder.toString());
    }
}
