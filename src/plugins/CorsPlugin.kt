package com.adamringhede.kateway.plugins

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod

class CorsPlugin : Plugin {

    override fun setup(app: Application) {
        app.apply {
            install(CORS) {
                method(HttpMethod.Options)
                method(HttpMethod.Put)
                method(HttpMethod.Delete)
                method(HttpMethod.Patch)
                header(HttpHeaders.Authorization)
                allowCredentials = true
                anyHost()
            }
        }
    }
}