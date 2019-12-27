package com.adamringhede.kateway

import io.ktor.client.HttpClient
import io.ktor.client.request.get

abstract class AdminAuthenticator {
    abstract suspend fun test(token: String?): Boolean
}

class SymmetricAdminAuthenticator(private val key: String) : AdminAuthenticator() {
    override suspend fun test(token: String?): Boolean {
        return token == key
    }
}

class NoAdminAuthenticator : AdminAuthenticator() {
    override suspend fun test(token: String?): Boolean {
        return true
    }
}

class GithubAdminAuthenticator(private val githubOrg: String) : AdminAuthenticator()  {
    private val client = HttpClient()
    private  data class GitHubOrg(val login: String)
    override suspend fun test(token: String?): Boolean {
        // TODO Prevent excessive requests by rate limiting this endpoint
        // TODO Cache responses from github.
        val orgs = client.get<List<GitHubOrg>>("https//api.github.com/user/orgs") {
            headers.append("Authorization", "token $token")
        }
        return orgs.find { it.login == githubOrg } != null
    }
}