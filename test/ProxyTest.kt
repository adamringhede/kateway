package com.adamringhede.apigateway

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProxyTest {

    @Test
    fun `test proxy request`() {
        servicesRepo.removeAll()
        servicesRepo.insert(Service("wikipedia", path = "/wikipedia", targets = listOf(ServiceTarget("https://www.wikipedia.org"))))
        get("/wikipedia").apply {
            assertEquals(HttpStatusCode.OK, response.status())
            assertTrue("<title>Wikipedia</title>" in response.content!!)
        }
    }

    @Test
    fun `test proxy request not found`() {
        servicesRepo.removeAll()
        get("/not-found").apply {
            assertEquals(HttpStatusCode.NotFound, response.status())
        }
    }

    private fun get(uri: String): TestApplicationCall {
        return withTestApplication({ proxyModule(testing = true) }) {
            handleRequest(HttpMethod.Get, uri)
        }
    }

    private fun post(uri: String, body: String): TestApplicationCall {
        return withTestApplication({ proxyModule(testing = true) }) {
            handleRequest(HttpMethod.Post, uri) {
                addHeader("Content-Type", "application/json")
                setBody(body)
            }
        }
    }

}

