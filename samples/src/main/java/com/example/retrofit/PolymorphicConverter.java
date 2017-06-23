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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import okhttp3.RequestBody;
import okio.Buffer;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * A converter for polymorphic request bodies. This may be useful for libraries and places where the
 * runtime types are unknown.
 * If the runtime types are known, consider using separate overloaded methods on the service
 * interface instead.
 *
 * For each service method, Retrofit caches converters for request bodies.
 * The following requires loading from another cache for each invocation of the service method.
 */
final class PolymorphicConverter {
  public static final class PolymorphicRequestBodyConverter<T>
      implements Converter<T, RequestBody> {

    private final Factory skipPast;
    private final Annotation[] parameterAnnotations;
    private final Annotation[] methodsAnnotations;
    private final Retrofit retrofit;
    private final Map<Class<?>, Converter<T, RequestBody>> cache = new LinkedHashMap<>();

    public static <T> Factory newFactory(final Class<T> baseType) {
      return new Factory() {
        @Override public Converter<?, RequestBody> requestBodyConverter(Type type,
            Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
          if (getRawType(type) != baseType) {
            return null;
          }
          return new PolymorphicRequestBodyConverter<>(this, parameterAnnotations,
              methodAnnotations, retrofit);
        }
      };
    }

    PolymorphicRequestBodyConverter(Factory skipPast, Annotation[] parameterAnnotations,
        Annotation[] methodsAnnotations, Retrofit retrofit) {
      this.skipPast = skipPast;
      this.parameterAnnotations = parameterAnnotations;
      this.methodsAnnotations = methodsAnnotations;
      this.retrofit = retrofit;
    }

    @Override public RequestBody convert(T value) throws IOException {
      Class<?> cls = value.getClass();
      Converter<T, RequestBody> requestBodyConverter;
      synchronized (cache) {
        requestBodyConverter = cache.get(cls);
      }
      if (requestBodyConverter == null) {
        requestBodyConverter =
            retrofit.nextRequestBodyConverter(skipPast, cls, parameterAnnotations,
                methodsAnnotations);
        synchronized (cache) {
          cache.put(cls, requestBodyConverter);
        }
      }
      return requestBodyConverter.convert(value);
    }
  }

  public interface Animal {
  }

  public static final class Dog implements Animal {
    private final String bark = "Woof!";
  }

  public interface Service {
    @POST("/") Call<Void> animal(@Body Animal animal);
  }

  public static void main(String[] args) throws IOException {
    Retrofit retrofit = new Retrofit.Builder().baseUrl("http://localhost")
        .addConverterFactory(PolymorphicRequestBodyConverter.newFactory(Animal.class))
        .addConverterFactory(MoshiConverterFactory.create())
        .build();
    Service service = retrofit.create(Service.class);

    Call<Void> call = service.animal(new Dog());
    Buffer buffer = new Buffer();
    call.request().body().writeTo(buffer);

    System.out.println(buffer.readUtf8());
  }
}
