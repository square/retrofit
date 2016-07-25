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

import android.support.annotation.IntDef;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.BufferedSource;

/**
 * Base class for all responses received from the server
 * <p>
 * Created by Cotuna Aurelian on 7/2/2016.
 */

public class BaseResponse extends ResponseBody implements Serializable {
    /**
     * Flag for no response or default one
     */
    public static final int NO_RESPONSE = 0;

    /**
     * Flag for the NDJSON format response
     */
    public static final int NDJSON_RESPONSE = 1;

    @IntDef({NDJSON_RESPONSE, NO_RESPONSE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ResponseType {
    }

    private int responseType;

    /**
     * Creates a new instance of this class, and sets it's response type to
     * {@link #NO_RESPONSE}. The response type can be altered in the future using the
     * {@link #setResponseType(int)} method.
     */
    public BaseResponse() {
        this.responseType = NO_RESPONSE;
    }

    /**
     * Creates a new instance of this class and sets it's response type accordingly.
     *
     * @param response One of the already defined response types {@link #NO_RESPONSE} or
     *                 {@link #NDJSON_RESPONSE}
     */
    public BaseResponse(@ResponseType int response) {
        this.responseType = response;
    }

    @Override
    public MediaType contentType() {
        return null;
    }

    @Override
    public long contentLength() {
        return 0;
    }

    @Override
    public BufferedSource source() {
        return null;
    }

    @ResponseType
    public int getResponseType() {
        return responseType;
    }

    public void setResponseType(@ResponseType int responseType) {
        this.responseType = responseType;
    }
}
