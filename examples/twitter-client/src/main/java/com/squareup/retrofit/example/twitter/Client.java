// Copyright 2012 Square, Inc.
package com.squareup.retrofit.example.twitter;

import com.google.gson.Gson;
import org.apache.http.impl.client.DefaultHttpClient;
import retrofit.http.GET;
import retrofit.http.GsonConverter;
import retrofit.http.RestAdapter;
import retrofit.http.Server;

import javax.inject.Named;
import java.util.List;

public class Client {
  private static final String API_URL = "https://api.twitter.com/1/";

  class Tweet {
    String text;
  }

  interface Twitter {
    @GET("statuses/user_timeline.json")
    List<Tweet> tweets(@Named("screen_name") String user);
  }

  public static void main(String... args) {
    // Create a very simple REST adapter which points the Twitter API endpoint.
    RestAdapter restAdapter = new RestAdapter.Builder()
        .setServer(new Server(API_URL))
        .setClient(new DefaultHttpClient())
        .setConverter(new GsonConverter(new Gson()))
        .build();

    // Create an instance of our Twitter API interface.
    Twitter twitter = restAdapter.create(Twitter.class);

    // Fetch and print a list of the 20 most recent tweets for a user.
    List<Tweet> tweets = twitter.tweets("horse_ebooks");
    for (Tweet tweet : tweets) {
      System.out.println(tweet.text);
    }
  }
}
