package com.example.retrofit.kotlin

import retrofit2.http.GET
import retrofit2.http.Path
import rx.Observable

interface GitHubService {
  @GET("users/{user}/repos")
  fun listRepos(@Path("user") user: String): Observable<List<RepoModel>>
}
