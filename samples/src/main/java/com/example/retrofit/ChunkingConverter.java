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
package com.example.retrofit;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.BufferedSink;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public final class ChunkingConverter {
  @Target(PARAMETER)
  @Retention(RUNTIME)
  @interface Chunked {}

  /**
   * A converter which removes known content lengths to force chunking when {@code @Chunked} is
   * present on {@code @Body} params.
   */
  static class ChunkingConverterFactory extends Converter.Factory {
    @Override
    public @Nullable Converter<Object, RequestBody> requestBodyConverter(
        Type type,
        Annotation[] parameterAnnotations,
        Annotation[] methodAnnotations,
        Retrofit retrofit) {
      boolean isBody = false;
      boolean isChunked = false;
      for (Annotation annotation : parameterAnnotations) {
        isBody |= annotation instanceof Body;
        isChunked |= annotation instanceof Chunked;
      }
      if (!isBody || !isChunked) {
        return null;
      }

      // Look up the real converter to delegate to.
      final Converter<Object, RequestBody> delegate =
          retrofit.nextRequestBodyConverter(this, type, parameterAnnotations, methodAnnotations);
      // Wrap it in a Converter which removes the content length from the delegate's body.
      return value -> {
        final RequestBody realBody = delegate.convert(value);
        return new RequestBody() {
          @Override
          public MediaType contentType() {
            return realBody.contentType();
          }

          @Override
          public void writeTo(BufferedSink sink) throws IOException {
            realBody.writeTo(sink);
          }
        };
      };
    }
  }

  static class Repo {
    final String owner;
    final String name;

    Repo(String owner, String name) {
      this.owner = owner;
      this.name = name;
    }
  }

  interface Service {
    @POST("/")
    Call<ResponseBody> sendNormal(@Body Repo repo);

    @POST("/")
    Call<ResponseBody> sendChunked(@Chunked @Body Repo repo);
  }

  public static void main(String... args) throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse());
    server.enqueue(new MockResponse());
    server.start();

    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(new ChunkingConverterFactory())
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    Service service = retrofit.create(Service.class);

    Repo retrofitRepo = new Repo("square", "retrofit");

    service.sendNormal(retrofitRepo).execute();
    RecordedRequest normalRequest = server.takeRequest();
    System.out.println(
        "Normal @Body Transfer-Encoding: " + normalRequest.getHeader("Transfer-Encoding"));

    service.sendChunked(retrofitRepo).execute();
    RecordedRequest chunkedRequest = server.takeRequest();
    System.out.println(
        "@Chunked @Body Transfer-Encoding: " + chunkedRequest.getHeader("Transfer-Encoding"));

    server.shutdown();
  }
}
