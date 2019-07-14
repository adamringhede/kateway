package com.adamringhede.apigateway

import com.adamringhede.apigateway.storage.EtcdServicesRepo
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.features.*
import io.ktor.routing.*
import io.ktor.http.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.etcd.jetcd.Client
import io.ktor.jackson.*

// TODO Start using an embedded server instead so that we also can have a server running for proxying.
fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

// TODO replace with dependency injection
private val etcdClient = Client.builder().endpoints("http://localhost:2379").build()
val servicesRepo = EtcdServicesRepo(etcdClient)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
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