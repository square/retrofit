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

import java.io.IOException;
import java.util.List;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

public final class SimpleService {
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
    Call<List<Contributor>> contributors(@Path("owner") String owner, @Path("repo") String repo);
  }

  public static void main(String... args) throws IOException {
    // Create a very simple REST adapter which points the GitHub API.
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    // Create an instance of our GitHub API interface.
    GitHub github = retrofit.create(GitHub.class);

    // Create a call instance for looking up Retrofit contributors.
    Call<List<Contributor>> call = github.contributors("square", "retrofit");

    // Fetch and print a list of the contributors to the library.
    List<Contributor> contributors = call.execute().body();
    for (Contributor contributor : contributors) {
      System.out.println(contributor.login + " (" + contributor.contributions + ")");
    }
  }

  private static void createDeadlock() {
        CasSpringBootAdminServerWebApplication test = new CasSpringBootAdminServerWebApplication();

        final A a = test.new A();
        final B b = test.new B();

        // Thread-1
        Runnable block1 = new Runnable() {
            public void run() {
                synchronized (a) {
                    try {
                        // Adding delay so that both threads can start trying to
                        // lock resources
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Thread-1 have A but need B also
                    synchronized (b) {
                        System.out.println("In block 1");
                    }
                }
            }
        };

        // Thread-2
        Runnable block2 = new Runnable() {
            public void run() {
                synchronized (b) {
                    // Thread-2 have B but need A also
                    synchronized (a) {
                        System.out.println("In block 2");
                    }
                }
            }
        };

        new Thread(block1).start();
        new Thread(block2).start();
    }

    // Resource A
    private class A {
        private int i = 10;

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }
    }

    // Resource B
    private class B {
        private int i = 20;

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }
    }
}
