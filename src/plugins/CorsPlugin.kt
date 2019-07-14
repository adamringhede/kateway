package com.adamringhede.apigateway.plugins

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
                header("MyCustomHeader")
                allowCredentials = true
                anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
            }
        }
    }
}