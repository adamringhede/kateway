package com.adamringhede.apigateway.plugins

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.*

class CompressionPlugin : Plugin {

    override fun setup(app: Application) {
        app.apply {
            install(Compression) {
                gzip {
                    priority = 1.0
                }
                deflate {
                    priority = 10.0
                    minimumSize(1024) // condition
                }
            }
        }
    }
}