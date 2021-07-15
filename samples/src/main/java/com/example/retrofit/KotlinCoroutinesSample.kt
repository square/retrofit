package com.example.retrofit

import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

class Contributor(val login: String, val contributions: Int)

interface GitHub {
    @GET("/repos/{owner}/{repo}/contributors")
    suspend fun contributors(@Path("owner") owner: String?, @Path("repo") repo: String?): List<Contributor>
}

fun main() {
    // Create a very simple REST adapter which points the GitHub API.
    val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    // Create an instance of our GitHub API interface.
    val github = retrofit.create(GitHub::class.java)

    runBlocking {
        // Fetch and print a list of the contributors to the library.
        val contributors = github.contributors("square", "retrofit")
        contributors.forEach { contributor ->
            println(contributor.login + " (" + contributor.contributions + ")")
        }
    }
}