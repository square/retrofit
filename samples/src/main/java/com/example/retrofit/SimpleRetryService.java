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
import java.util.List;

import retrofit2.Call;
import retrofit2.CustomCallback;
import retrofit2.Retrofit;
import retrofit2.RetryHelper;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;


public final class SimpleRetryService {
  public static final String API_URL = "https://api.github.com";

  public static class Contributor {
    public final String login;
    public final int contributions;

    public Contributor(String login, int contributions) {
      this.login = login;
      this.contributions = contributions;
    }
  }

  public interface GitHub {
    @GET("/repos/{owner}/{repo}/contributors")
    Call<List<Contributor>> contributors(
        @Path("owner") String owner,
        @Path("repo") String repo);
  }

  public static void main(String... args) throws IOException {
    // Create a very simple REST adapter which points the GitHub API.
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build();

    // Create an instance of our GitHub API interface.
    GitHub github = retrofit.create(GitHub.class);

    // Create a call instance for looking up Retrofit contributors.
    Call<List<Contributor>> call = github.contributors("square", "retrofit");

    // Fetch and print a list of the contributors to the library.
    RetryHelper.setSuccessCode(200);

    RetryHelper.enqueueRetry(responseCall, 3, new CustomCallback<List<Contributor>>() {
      @Override
      public void onFailResponse(int errorCode, Call<List<Contributor>> call, Response<List<Contributor>> response) {
        Log.e(TAG, "onFailResponse() called with: errorCode = [" + errorCode + "], call = [" + call + "], response = [" + response + "]");
      }

      @Override
      public void onResponse(Call<List<Contributor>> call, Response<List<Contributor>> response) {
        Log.e(TAG, "Success" + response.code());
        for (Contributor contributor : contributors) {
          System.out.println(contributor.login + " (" + contributor.contributions + ")");
        }
      }

      @Override
      public void onFailure(Call<List<Contributor>> call, Throwable t) {
        Log.e(TAG, "onFailure: " + t.getMessage());
      }
    });

  }
}
