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
package retrofit.http;

import com.squareup.okhttp.HttpUrl;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import retrofit.Retrofit;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * URL resolved against the {@linkplain Retrofit#baseUrl() base URL}.
 * <pre>
 * &#64;GET
 * void list(@Url String url);
 * </pre>
 * <p>
 * See {@linkplain retrofit.Retrofit.Builder#baseUrl(HttpUrl) base URL} for details of how
 * the value will be resolved against a base URL to create the full endpoint URL.
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Url {
}
