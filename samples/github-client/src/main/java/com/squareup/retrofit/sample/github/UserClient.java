// Copyright 2012 Square, Inc.
package com.squareup.retrofit.sample.github;

import retrofit.http.BaseUrl;
import retrofit.http.GET;
import retrofit.http.Name;
import retrofit.http.RestAdapter;
import retrofit.http.client.Response;

import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.InputStream;

public class UserClient {
  private static final String API_URL = "https://api.github.com";

  class User {
    String login;

    @SerializedName("avatar_url")
    String avatarUrl;
  }

  interface GitHub {

    @GET("/users/{user}")
    User getUser(
        @Name("user") String user
    );

    @GET("/")
    Response getAvatarImage(
        @BaseUrl String avatarUrl
    );
  }

  public static void main(String... args) {
    // Create a very simple REST adapter which points the GitHub API endpoint.
    RestAdapter restAdapter = new RestAdapter.Builder()
        .setServer(API_URL)
        .build();

    // Create an instance of our GitHub API interface.
    GitHub github = restAdapter.create(GitHub.class);

    User user = github.getUser("JakeWharton");
    Response avatarResponse = github.getAvatarImage(user.avatarUrl);
    InputStream in = null;
    try {
        in = avatarResponse.getBody().in();
        System.out.println("we got the avatar!");
    } catch (IOException e) {
        e.printStackTrace();
    } finally {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
  }
}
