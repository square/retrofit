// Copyright 2012 Square, Inc.
package com.squareup.retrofit.sample.twitter;

import java.util.List;
import retrofit.http.GET;
import retrofit.http.Name;
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
        @Name("owner") String owner,
        @Name("repo") String repo
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
