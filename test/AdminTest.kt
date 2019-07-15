package com.adamringhede.kateway


import com.adamringhede.kateway.storage.MockServicesRepo
import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue



class AdminTest {
    private val servicesRepo = MockServicesRepo()

    @Test
    fun `test add service`() {
        post("/services", """{"name": "example", "path": "/example", "targets": [{"url": "http://example.com:8080"}]}""").apply {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    fun `test add service missing parameter`() {
        post("/services", "{}").apply {
            assertEquals(HttpStatusCode.BadRequest, response.status())
        }
    }

    @Test
    fun `test list services`() {
        servicesRepo.removeAll()
        servicesRepo.insert(Service("example", path = "/example", targets = listOf(ServiceTarget("http://example.com:8080"))))
        servicesRepo.insert(Service("example2", path = "/example2", targets = emptyList()))
        get("/services").apply {
            assertEquals(HttpStatusCode.OK, response.status())
            val services = jacksonObjectMapper().readValue<List<Service>>(response.content!!)
            assertEquals(2, services.size)
        }
    }

    @Test
    fun `test remove service`() {
        servicesRepo.removeAll()
        servicesRepo.insert(Service("example", path = "/example", targets = emptyList()))
        delete("/services/example").apply {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(0, servicesRepo.findAll().size)
        }
    }

    private fun get(uri: String): TestApplicationCall {
        return withTestApplication({ adminModule(testing = true, servicesRepo = servicesRepo) }) {
            handleRequest(HttpMethod.Get, uri)
        }
    }

    private fun delete(uri: String): TestApplicationCall {
        return withTestApplication({ adminModule(testing = true, servicesRepo = servicesRepo) }) {
            handleRequest(HttpMethod.Delete, uri)
        }
    }

    private fun post(uri: String, body: String): TestApplicationCall {
        return withTestApplication({ adminModule(testing = true, servicesRepo = servicesRepo) }) {
            handleRequest(HttpMethod.Post, uri) {
                addHeader("Content-Type", "application/json")
                setBody(body)
            }
        }
    }

}

