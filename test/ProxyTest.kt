package com.adamringhede.kateway

import com.adamringhede.kateway.helpers.ApiTest
import com.adamringhede.kateway.storage.MockServicesRepo
import io.ktor.application.Application
import io.ktor.http.HttpStatusCode
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class ProxyTest : ApiTest() {
    private val servicesRepo = MockServicesRepo()

    override fun setupModule(app: Application) {
        app.proxyModule(testing = true, servicesRepo = servicesRepo)
    }

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

}
