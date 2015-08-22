// Copyright 2013 Square, Inc.
package com.example.retrofit;

import com.example.retrofit.SimpleService.Contributor;
import com.example.retrofit.SimpleService.GitHub;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit.Call;
import retrofit.Retrofit;
import retrofit.mock.CallBehaviorAdapter;
import retrofit.mock.Calls;
import retrofit.mock.MockRetrofit;
import retrofit.mock.NetworkBehavior;

/**
 * An example of using {@link MockRetrofit} to create a mock service implementation with
 * fake data. This re-uses the GitHub service from {@link SimpleService} for its mocking.
 */
public final class SimpleMockService {
  /** A mock implementation of the {@link GitHub} API interface. */
  static final class MockGitHub implements GitHub {
    private final Map<String, Map<String, List<Contributor>>> ownerRepoContributors;

    public MockGitHub() {
      ownerRepoContributors = new LinkedHashMap<>();

      // Seed some mock data.
      addContributor("square", "retrofit", "John Doe", 12);
      addContributor("square", "retrofit", "Bob Smith", 2);
      addContributor("square", "retrofit", "Big Bird", 40);
      addContributor("square", "picasso", "Proposition Joe", 39);
      addContributor("square", "picasso", "Keiser Soze", 152);
    }

    @Override public Call<List<Contributor>> contributors(String owner, String repo) {
      List<Contributor> response = Collections.emptyList();
      Map<String, List<Contributor>> repoContributors = ownerRepoContributors.get(owner);
      if (repoContributors != null) {
        List<Contributor> contributors = repoContributors.get(repo);
        if (contributors != null) {
          response = contributors;
        }
      }
      return Calls.response(response);
    }

    public void addContributor(String owner, String repo, String name, int contributions) {
      Map<String, List<Contributor>> repoContributors = ownerRepoContributors.get(owner);
      if (repoContributors == null) {
        repoContributors = new LinkedHashMap<>();
        ownerRepoContributors.put(owner, repoContributors);
      }
      List<Contributor> contributors = repoContributors.get(repo);
      if (contributors == null) {
        contributors = new ArrayList<>();
        repoContributors.put(repo, contributors);
      }
      contributors.add(new Contributor(name, contributions));
    }
  }

  public static void main(String... args) throws IOException {
    // Create a very simple Retrofit adapter which points the GitHub API.
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(SimpleService.API_URL)
        .build();

    // Create the Behavior object which manages the fake behavior and the background executor.
    NetworkBehavior behavior = NetworkBehavior.create();
    ExecutorService bg = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
        .setNameFormat("mock-retrofit-%d")
        .setDaemon(true)
        .build());

    // Create the mock implementation and use MockRetrofit to apply the behavior to it.
    MockRetrofit mockRetrofit = new MockRetrofit(behavior, new CallBehaviorAdapter(retrofit, bg));
    MockGitHub mockGitHub = new MockGitHub();
    GitHub gitHub = mockRetrofit.create(GitHub.class, mockGitHub);

    // Query for some contributors for a few repositories.
    printContributors(gitHub, "square", "retrofit");
    printContributors(gitHub, "square", "picasso");

    // Using the mock object, add some additional mock data.
    System.out.println("Adding more mock data...\n");
    mockGitHub.addContributor("square", "retrofit", "Foo Bar", 61);
    mockGitHub.addContributor("square", "picasso", "Kit Kat", 53);

    // Query for the contributors again so we can see the mock data that was added.
    printContributors(gitHub, "square", "retrofit");
    printContributors(gitHub, "square", "picasso");
  }

  private static void printContributors(GitHub gitHub, String owner, String repo)
      throws IOException {
    System.out.println(String.format("== Contributors for %s/%s ==", owner, repo));
    Call<List<Contributor>> contributors = gitHub.contributors(owner, repo);
    for (Contributor contributor : contributors.execute().body()) {
      System.out.println(contributor.login + " (" + contributor.contributions + ")");
    }
    System.out.println();
  }
}
