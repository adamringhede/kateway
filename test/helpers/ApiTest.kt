package com.adamringhede.kateway.helpers

import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication


abstract class ApiTest {
    protected fun get(uri: String): TestApplicationCall {
        return withTestApplication({ setupModule(this) }) {
            handleRequest(HttpMethod.Get, uri)
        }
    }

    protected fun post(uri: String, body: String): TestApplicationCall {
        return withTestApplication({ setupModule(this) }) {
            handleRequest(HttpMethod.Post, uri) {
                addHeader("Content-Type", "application/json")
                setBody(body)
            }
        }
    }

    protected fun delete(uri: String): TestApplicationCall {
        return withTestApplication({ setupModule(this) }) {
            handleRequest(HttpMethod.Delete, uri)
        }
    }

    abstract fun setupModule(app: Application)
}

