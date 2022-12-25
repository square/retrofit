package com.example.retrofit;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.BaseUrl;
import retrofit2.http.GET;

public final class MultiRemotes {

  public interface Remote {
    @GET("robots.txt")
    Call<ResponseBody> robots();
  }

  @BaseUrl("https://www.google.com/")
  public interface Remote1 {
    @GET("robots.txt")
    Call<ResponseBody> robots();
  }

  @BaseUrl("https://www.facebook.com/")
  public interface Remote2 {
    @GET("robots.txt")
    Call<ResponseBody> robots();
  }

  @SuppressWarnings("UnusedVariable")
  public static void main(String... args) throws IOException {
    Retrofit retrofit =
      new Retrofit.Builder().baseUrl("http://www.github.com/").build();

    Remote remote = retrofit.create(Remote.class);
    Response<ResponseBody> response = remote.robots().execute();
    System.out.println("Response from: " + response.raw().request().url());
    System.out.println(response.body().string());

    Remote1 remote1 = retrofit.create(Remote1.class);
    Response<ResponseBody> response1 = remote1.robots().execute();
    System.out.println("Response from: " + response1.raw().request().url());
    System.out.println(response1.body().string());

    Remote2 remote2 = retrofit.create(Remote2.class);
    Response<ResponseBody> response2 = remote2.robots().execute();
    System.out.println("Response from: " + response2.raw().request().url());
    System.out.println(response2.body().string());
  }
}
