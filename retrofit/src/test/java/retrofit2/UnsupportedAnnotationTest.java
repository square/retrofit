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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public final class UnsupportedAnnotationTest {

    @Rule public final MockWebServer server = new MockWebServer();
    private PrintStream console = null;
    private ByteArrayOutputStream bytes = null;

    interface Annotated {
        @GET("/")
        @Foo
        Call<String> method();

        @POST("/")
        Call<ResponseBody> bodyParameter(@Foo @Body String param);

        @GET("/")
        Call<ResponseBody> queryParameter(@Foo @Query("foo") Object foo);

        @Target({PARAMETER,METHOD})
        @Retention(RUNTIME)
        @interface Foo {}
    }

    @Before
    public void setUp() throws Exception {
        bytes = new ByteArrayOutputStream();
        console = System.out;
        System.setOut(new PrintStream(bytes));
    }

    @After
    public void tearDown() throws Exception {
        System.setOut(console);
    }

    @Test
    public void testUnsupportedAnnotation() throws NoSuchMethodException {
        Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(server.url("/"))
                        .build();
        Annotated annotated = retrofit.create(Annotated.class);
        annotated.bodyParameter(null); // Trigger internal setup.
        Annotation[] annotations = annotated.getClass().getDeclaredMethod("bodyParameter", String.class).getDeclaredAnnotations();

        assertTrue(bytes.toString().contains("Unsupported annotation @Foo"));
        assertThat(annotations).hasAtLeastOneElementOfType(Annotated.Foo.class);

    }

    @Test
    public void testUnsupportedAnnotation2() throws NoSuchMethodException {
        Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(server.url("/"))
                        .build();
        Annotated annotated = retrofit.create(Annotated.class);
        annotated.queryParameter(null); // Trigger internal setup.
        Annotation[] annotations = annotated.getClass().getDeclaredMethod("queryParameter", Object.class).getDeclaredAnnotations();

        assertTrue(bytes.toString().contains("Unsupported annotation @Foo"));
        assertThat(annotations).hasAtLeastOneElementOfType(Annotated.Foo.class);
    }



}
