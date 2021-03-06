package com.adamringhede.kateway


import com.adamringhede.kateway.helpers.ApiTest
import com.adamringhede.kateway.storage.MockServicesRepo
import io.ktor.http.*
import kotlin.test.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.Application


class AdminTest : ApiTest() {
    private val servicesRepo = MockServicesRepo()

    override fun setupModule(app: Application) {
        app.adminModule(servicesRepo = servicesRepo)
    }

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
        get("/services").run {
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

}

