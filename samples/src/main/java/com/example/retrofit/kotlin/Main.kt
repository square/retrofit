package com.example.retrofit.kotlin

import rx.schedulers.Schedulers
import rx.Observer

fun main() {
  val user = "Amir-yazdanmanesh"
  fetchRepositories(user)
}


fun fetchRepositories(user: String) {
  RetrofitInstance.api.listRepos(user)
    .subscribeOn(Schedulers.io())
    .observeOn(Schedulers.computation())
    .subscribe(
      object : Observer<List<RepoModel>> {
        override fun onNext(repos: List<RepoModel>) {
          repos.forEach { println(it.name) }
        }

        override fun onError(e: Throwable) {
          println("Error: ${e.message}")
        }

        override fun onCompleted() {
          println("Request complete")
        }
      }
    )
}
