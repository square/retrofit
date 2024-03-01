/*
 * Copyright (C) 2024 Square, Inc.
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
package retrofit2.keeper

import com.google.common.truth.Truth.assertAbout
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory.javaSource
import java.nio.charset.StandardCharsets.UTF_8
import javax.tools.StandardLocation.CLASS_OUTPUT
import org.junit.Test

class RetrofitResponseTypeKeepProcessorTest {
  @Test
  fun allHttpMethods() {
    val service = JavaFileObjects.forSourceString(
      "test.Service",
      """
      package test;
      import retrofit2.*;
      import retrofit2.http.*;

      class DeleteUser {}
      class GetUser {}
      class HeadUser {}
      class HttpUser {}
      class OptionsUser {}
      class PatchUser {}
      class PostUser {}
      class PutUser {}

      interface Service {
        @DELETE("/") Call<DeleteUser> delete();
        @GET("/") Call<GetUser> get();
        @HEAD("/") Call<HeadUser> head();
        @HTTP(method = "CUSTOM", path = "/") Call<HttpUser> http();
        @OPTIONS("/") Call<OptionsUser> options();
        @PATCH("/") Call<PatchUser> patch();
        @POST("/") Call<PostUser> post();
        @PUT("/") Call<PutUser> put();
      }
    """.trimIndent(),
    )

    assertAbout(javaSource())
      .that(service)
      .processedWith(RetrofitResponseTypeKeepProcessor())
      .compilesWithoutError()
      .and()
      .generatesFileNamed(
        CLASS_OUTPUT,
        "",
        "META-INF/proguard/retrofit-response-type-keeper-test.Service.pro",
      ).withStringContents(
        UTF_8,
        """
        |# test.Service
        |-keep,allowobfuscation,allowoptimization class retrofit2.Call
        |-keep,allowobfuscation,allowoptimization class test.DeleteUser
        |-keep,allowobfuscation,allowoptimization class test.GetUser
        |-keep,allowobfuscation,allowoptimization class test.HeadUser
        |-keep,allowobfuscation,allowoptimization class test.HttpUser
        |-keep,allowobfuscation,allowoptimization class test.OptionsUser
        |-keep,allowobfuscation,allowoptimization class test.PatchUser
        |-keep,allowobfuscation,allowoptimization class test.PostUser
        |-keep,allowobfuscation,allowoptimization class test.PutUser
        |
        """.trimMargin(),
      )
  }

  @Test
  fun nesting() {
    val service = JavaFileObjects.forSourceString(
      "test.Service",
      """
      package test;
      import retrofit2.*;
      import retrofit2.http.*;

      class One<T> {}
      class Two<T> {}
      class Three {}

      interface Service {
        @GET("/") Call<One<Two<Three>>> get();
      }
    """.trimIndent(),
    )

    assertAbout(javaSource())
      .that(service)
      .processedWith(RetrofitResponseTypeKeepProcessor())
      .compilesWithoutError()
      .and()
      .generatesFileNamed(
        CLASS_OUTPUT,
        "",
        "META-INF/proguard/retrofit-response-type-keeper-test.Service.pro",
      ).withStringContents(
        UTF_8,
        """
        |# test.Service
        |-keep,allowobfuscation,allowoptimization class retrofit2.Call
        |-keep,allowobfuscation,allowoptimization class test.One
        |-keep,allowobfuscation,allowoptimization class test.Three
        |-keep,allowobfuscation,allowoptimization class test.Two
        |
        """.trimMargin(),
      )
  }

  @Test
  fun kotlinSuspend() {
    val service = JavaFileObjects.forSourceString(
      "test.Service",
      """
      package test;
      import kotlin.coroutines.Continuation;
      import retrofit2.*;
      import retrofit2.http.*;

      class Body {}

      interface Service {
        @GET("/") Object get(Continuation<? extends Body> c);
      }
    """.trimIndent(),
    )

    assertAbout(javaSource())
      .that(service)
      .processedWith(RetrofitResponseTypeKeepProcessor())
      .compilesWithoutError()
      .and()
      .generatesFileNamed(
        CLASS_OUTPUT,
        "",
        "META-INF/proguard/retrofit-response-type-keeper-test.Service.pro",
      ).withStringContents(
        UTF_8,
        """
        |# test.Service
        |-keep,allowobfuscation,allowoptimization class java.lang.Object
        |-keep,allowobfuscation,allowoptimization class test.Body
        |
        """.trimMargin(),
      )
  }
}
