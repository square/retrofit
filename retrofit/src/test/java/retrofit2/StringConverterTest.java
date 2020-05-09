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
package retrofit2;

import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.StringContains.containsString;

public final class StringConverterTest {

    @Rule public final MockWebServer server = new MockWebServer();

    @Rule public ExpectedException exception = ExpectedException.none();

    interface Annotated {

        @GET("/")
        Call<ResponseBody> queryString(@Query("word") String word);

        @GET("/")
        Call<ResponseBody> queryObject(@Foo @Query("foo") Object foo);

        @Target({PARAMETER, METHOD})
        @Retention(RUNTIME)
        @interface Foo {}
    }

    @Test
    public void queryObjectWithStringConverter() {
        final AtomicReference<Annotation[]> annotationsRef = new AtomicReference<>();
        class MyConverterFactory extends Converter.Factory {
            @Override
            public Converter<?, String> stringConverter(
                    Type type, Annotation[] annotations, Retrofit retrofit) {
                annotationsRef.set(annotations);

                return (Converter<Object, String>) String::valueOf;
            }
        }
        Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(server.url("/"))
                        .addConverterFactory(new MyConverterFactory())
                        .build();
        Annotated annotated = retrofit.create(Annotated.class);
        annotated.queryObject(null); // Trigger internal setup.

        Annotation[] annotations = annotationsRef.get();
        assertThat(annotations).hasAtLeastOneElementOfType(Annotated.Foo.class);
    }

    @Test
    public void queryObjectWithoutStringConverter() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("Unable to create @Query converter for class java.lang.Object (parameter #1)"));
        Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(server.url("/"))
                        .build();
        Annotated annotated = retrofit.create(Annotated.class);
        annotated.queryObject(null); // Trigger internal setup.
    }

    @Test
    public void queryStringWithoutStringConverter() {
        Retrofit retrofit = new Retrofit.Builder().baseUrl(server.url("/")).build();
        Annotated annotated = retrofit.create(Annotated.class);
        try {
            annotated.queryString(null); // Trigger internal setup.
            assertTrue(true);
        } catch (Exception e) {
            assertTrue(false);
        }
    }
}
