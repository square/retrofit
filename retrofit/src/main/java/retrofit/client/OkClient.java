/*
 * Copyright (C) 2013 Square, Inc.
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
package retrofit.client;

import com.squareup.okhttp.OkHttpClient;
import retrofit.mime.TypedInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Retrofit client that uses OkHttp for communication.
 */
public class OkClient extends UrlConnectionClient {
    private static OkHttpClient generateDefaultOkHttp() {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(Defaults.CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        client.setReadTimeout(Defaults.READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        return client;
    }

    private final OkHttpClient client;

    public OkClient() {
        this(generateDefaultOkHttp());
    }

    public OkClient(OkHttpClient client) {
        this.client = client;
    }

    @Override public Response execute(Request request) throws IOException {
        com.squareup.okhttp.Request requestOk = new com.squareup.okhttp.Request.Builder()
        .url(request.getUrl())
        .build();

        com.squareup.okhttp.Response response = client.newCall(requestOk).execute();

        List<Header> headerList = new ArrayList<Header>();
        for (int i = 0; i < response.headers().size(); i++) {
            Header header = new Header(response.headers().name(i),
                                       response.headers().value(i));
            headerList.add(header);
        }

        TypedInput responseBody = new TypedInputStream(
            response.body().contentType().type(),
            response.body().contentLength(),
            response.body().byteStream());

        Response resp = new Response(request.getUrl(),
                                     response.code(), response.message(),
                                     headerList, responseBody);
        return resp;
    }
}
