package com.adamringhede.kateway.plugins

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.routing.Routing

interface Plugin {
    enum class Interception {
        DONE, PASS_THROUGH
    }

    fun setup(app: Application)

    fun intercept(call: ApplicationCall, requestBuilder: HttpRequestBuilder): Interception {
        return Interception.PASS_THROUGH
    }

}