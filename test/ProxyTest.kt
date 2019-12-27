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
        app.proxyModule(
            servicesRepo = servicesRepo,
            requestAuthenticator = MockRequestAuthenticator()
        )
    }

    @Test
    fun `test proxy request`() {
        servicesRepo.removeAll()
        servicesRepo.insert(Service("wikipedia", path = "/wikipedia", public = true, targets = listOf(ServiceTarget("https://www.wikipedia.org"))))
        get("/wikipedia").apply {
            assertEquals(HttpStatusCode.OK, response.status())
            assertTrue("<title>Wikipedia" in response.content!!)
        }
    }

    @Test
    fun `test proxy post request`() {
        servicesRepo.removeAll()
        servicesRepo.insert(Service("dev/null", path = "/dev/null", public = true,  targets = listOf(ServiceTarget("http://devnull-as-a-service.com/dev/null"))))
        post("/dev/null", """{"msg": "Hello World"}""").apply {
            assertEquals(HttpStatusCode.OK, response.status())
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
