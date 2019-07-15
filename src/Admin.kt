package com.adamringhede.kateway

import com.adamringhede.kateway.storage.ServicesRepo
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing

fun Application.adminModule(testing: Boolean = false, servicesRepo: ServicesRepo) {
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

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

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    routing {

        post("/services") {
            call.receiveValid<Service>()?.let { service ->
                servicesRepo.insert(service)
                call.respond(service)
            }
        }

        get("/services") {
            call.respond(servicesRepo.findAll())
        }

        delete("/services/{name}") {
            val service = servicesRepo.findAll().find { it.name == call.parameters["name"] }
            if (service == null) {
                call.respondText("Could not find service", status = HttpStatusCode.NotFound)
            } else {
                servicesRepo.remove(service.name)
                call.respondText("Removed service")
            }
        }
    }
}

suspend inline fun <reified T : Any> ApplicationCall.receiveValid(): T? {
    try {
        return receive(T::class)
    } catch (e: MissingKotlinParameterException) {
        respondText(status = HttpStatusCode.BadRequest, text = "Missing parameter: ${e.parameter.name}")
    }
    return null
}