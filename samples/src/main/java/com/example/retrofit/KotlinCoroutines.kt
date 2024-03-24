package com.example.retrofit

import retrofit2.ResultCallAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Path

data class Contributor(val login: String, val contributions: Int)

interface GitHub {
  @GET("/repos/{owner}/{repo}/contributors")
  suspend fun getContributors(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
  ): List<Contributor>

  @GET("/repos/{owner}/{repo}/contributors")
  suspend fun getContributorsWithResult(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
  ): Result<List<Contributor>>
}

suspend fun main() {
  val retrofit = Retrofit.Builder()
    .baseUrl("https://api.github.com")
    .addCallAdapterFactory(ResultCallAdapterFactory.create())
    .addConverterFactory(GsonConverterFactory.create())
    .build()
  val github: GitHub = retrofit.create()

  println("Request without Result using")
  try {
    github.getContributors("square", "retrofit").forEach { contributor ->
      println(contributor)
    }
  } catch (e: Exception) {
    println("An error occurred when not using Result: $e")
  }

  println("Request with Result using")
  github.getContributorsWithResult("square", "retrofit")
    .onSuccess {
      it.forEach { contributor ->
        println(contributor)
      }
    }
    .onFailure {
      println("An error occurred when using Result: $it")
    }
}
