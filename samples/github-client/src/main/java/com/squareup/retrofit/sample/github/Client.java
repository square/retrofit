/*
 * Copyright (C) 2012 Square, Inc.
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
package com.squareup.retrofit.sample.github;

import java.util.List;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.RestAdapter;

public class Client {
  private static final String API_URL = "https://api.github.com";

  class Contributor {
    String login;
    int contributions;
  }

  interface GitHub {
    @GET("/repos/{owner}/{repo}/contributors")
    List<Contributor> contributors(
        @Path("owner") String owner,
        @Path("repo") String repo
    );
  }

  public static void main(String... args) {
    // Create a very simple REST adapter which points the GitHub API endpoint.
    RestAdapter restAdapter = new RestAdapter.Builder()
        .setServer(API_URL)
        .build();

    // Create an instance of our GitHub API interface.
    GitHub github = restAdapter.create(GitHub.class);

    // Fetch and print a list of the contributors to this library.
    List<Contributor> contributors = github.contributors("square", "retrofit");
    for (Contributor contributor : contributors) {
      System.out.println(contributor.login + " (" + contributor.contributions + ")");
    }
  }
}
