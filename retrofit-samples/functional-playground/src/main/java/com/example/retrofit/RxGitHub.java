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
package com.example.retrofit;

import java.util.List;
import retrofit.RestAdapter;
import retrofit.http.GET;
import retrofit.http.Path;
import rx.Observable;
import rx.Observer;
import rx.util.functions.Func1;

public class RxGitHub {
  private static final String API_URL = "https://api.github.com";

  static class Contributor {
    String login;
    int contributions;
  }

  interface GitHub extends Func1<String, List<Contributor>> {

    @Override @GET("/repos/square/{repo}/contributors")
    List<Contributor> call(@Path("repo") String repo);
  }

  public static void main(String... args) {
    // Create a very simple REST adapter which points the GitHub API endpoint.
    RestAdapter restAdapter = new RestAdapter.Builder().setServer(API_URL).build();

    // Create an instance of our GitHub API interface.
    GitHub github = restAdapter.create(GitHub.class);

    // Use github as a mapping step
    Observable.from("okhttp", "retrofit") //
        .map(github) //
        .flatMap(new Func1<List<Contributor>, Observable<Contributor>>() {
          @Override public Observable<Contributor> call(List<Contributor> contributors) {
            return Observable.from(contributors);
          }
        })//
        .subscribe(new Observer<Contributor>() {
          @Override public void onCompleted() {
          }

          @Override public void onError(Throwable e) {
            e.printStackTrace();
          }

          @Override public void onNext(Contributor contributor) {
            System.out.println(contributor.login + " (" + contributor.contributions + ")");
          }
        });
  }
}
