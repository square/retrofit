package com.example.retrofit.kotlin

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
  private val retrofit by lazy {
    Retrofit.Builder()
      .baseUrl("https://api.github.com/")
      .addConverterFactory(GsonConverterFactory.create())
      .build()
  }

  val api: GitHubService by lazy {
    retrofit.create(GitHubService::class.java)
  }
}
