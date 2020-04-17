/*
 * Copyright (C) 2017 Square, Inc.
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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import retrofit2.http.GET;

final class AnnotatedConverters {
  public static final class AnnotatedConverterFactory extends Converter.Factory {
    private final Map<Class<? extends Annotation>, Converter.Factory> factories;

    public static final class Builder {
      private final Map<Class<? extends Annotation>, Converter.Factory> factories =
          new LinkedHashMap<>();

      public Builder add(Class<? extends Annotation> cls, Converter.Factory factory) {
        if (cls == null) {
          throw new NullPointerException("cls == null");
        }
        if (factory == null) {
          throw new NullPointerException("factory == null");
        }
        factories.put(cls, factory);
        return this;
      }

      public AnnotatedConverterFactory build() {
        return new AnnotatedConverterFactory(factories);
      }
    }

    AnnotatedConverterFactory(Map<Class<? extends Annotation>, Converter.Factory> factories) {
      this.factories = new LinkedHashMap<>(factories);
    }

    @Override
    public @Nullable Converter<ResponseBody, ?> responseBodyConverter(
        Type type, Annotation[] annotations, Retrofit retrofit) {
      for (Annotation annotation : annotations) {
        Converter.Factory factory = factories.get(annotation.annotationType());
        if (factory != null) {
          return factory.responseBodyConverter(type, annotations, retrofit);
        }
      }
      return null;
    }

    @Override
    public @Nullable Converter<?, RequestBody> requestBodyConverter(
        Type type,
        Annotation[] parameterAnnotations,
        Annotation[] methodAnnotations,
        Retrofit retrofit) {
      for (Annotation annotation : parameterAnnotations) {
        Converter.Factory factory = factories.get(annotation.annotationType());
        if (factory != null) {
          return factory.requestBodyConverter(
              type, parameterAnnotations, methodAnnotations, retrofit);
        }
      }
      return null;
    }
  }

  @Retention(RUNTIME)
  public @interface Moshi {}

  @Retention(RUNTIME)
  public @interface Gson {}

  @Retention(RUNTIME)
  public @interface SimpleXml {}

  @Default(value = DefaultType.FIELD)
  static final class Library {
    @Attribute String name;
  }

  interface Service {
    @GET("/")
    @Moshi
    Call<Library> exampleMoshi();

    @GET("/")
    @Gson
    Call<Library> exampleGson();

    @GET("/")
    @SimpleXml
    Call<Library> exampleSimpleXml();

    @GET("/")
    Call<Library> exampleDefault();
  }

  public static void main(String... args) throws IOException {
    MockWebServer server = new MockWebServer();
    server.start();
    server.enqueue(new MockResponse().setBody("{\"name\": \"Moshi\"}"));
    server.enqueue(new MockResponse().setBody("{\"name\": \"Gson\"}"));
    server.enqueue(new MockResponse().setBody("<user name=\"SimpleXML\"/>"));
    server.enqueue(new MockResponse().setBody("{\"name\": \"Gson\"}"));

    com.squareup.moshi.Moshi moshi = new com.squareup.moshi.Moshi.Builder().build();
    com.google.gson.Gson gson = new GsonBuilder().create();
    MoshiConverterFactory moshiConverterFactory = MoshiConverterFactory.create(moshi);
    GsonConverterFactory gsonConverterFactory = GsonConverterFactory.create(gson);
    SimpleXmlConverterFactory simpleXmlConverterFactory = SimpleXmlConverterFactory.create();
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(
                new AnnotatedConverterFactory.Builder()
                    .add(Moshi.class, moshiConverterFactory)
                    .add(Gson.class, gsonConverterFactory)
                    .add(SimpleXml.class, simpleXmlConverterFactory)
                    .build())
            .addConverterFactory(gsonConverterFactory)
            .build();
    Service service = retrofit.create(Service.class);

    Library library1 = service.exampleMoshi().execute().body();
    System.out.println("Library 1: " + library1.name);

    Library library2 = service.exampleGson().execute().body();
    System.out.println("Library 2: " + library2.name);

    Library library3 = service.exampleSimpleXml().execute().body();
    System.out.println("Library 3: " + library3.name);

    Library library4 = service.exampleDefault().execute().body();
    System.out.println("Library 4: " + library4.name);

    server.shutdown();
  }
}
