package com.adamringhede.apigateway

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.jackson.jackson
import io.ktor.request.httpMethod
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.util.StringValues
import io.ktor.util.filter
import io.ktor.util.toMap
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.copyAndClose
import org.eclipse.jetty.http.HttpStatus


fun Application.proxyModule(testing: Boolean = false) {
    // TODO Compression should be added as a plugin
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

    // TODO CORS should be added as a plugin
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

    // TOOD Will using a single client be bad for performance?
    val client = HttpClient()
    intercept(ApplicationCallPipeline.Call) {
        // TODO making a database request on each query is inefficient. it should be cached in memory.
        val services = servicesRepo.findAll()
        val service = services.find { call.request.uri.startsWith(it.path, true) }
        val target = service?.targets?.firstOrNull()

        if (service != null && target != null) {
            val requestBuilder = HttpRequestBuilder().apply {
                url.takeFrom(target.url)
                method = call.request.httpMethod
                headers.appendAll(call.request.headers)
                //body = call.request.receiveChannel()
            }

            val result = client.execute(requestBuilder)

            val proxiedHeaders = result.response.headers
            val contentType = proxiedHeaders[HttpHeaders.ContentType]
            val contentLength = proxiedHeaders[HttpHeaders.ContentLength]

            call.respond(object : OutgoingContent.WriteChannelContent() {
                override val contentLength: Long? = contentLength?.toLong()
                override val contentType: ContentType? = contentType?.let { ContentType.parse(it) }
                override val headers: Headers = Headers.build {
                    appendAll(proxiedHeaders.filter { key, _ -> !key.equals(HttpHeaders.ContentType, ignoreCase = true) && !key.equals(HttpHeaders.ContentLength, ignoreCase = true) })
                }
                override val status: HttpStatusCode? = result.response.status
                override suspend fun writeTo(channel: ByteWriteChannel) {
                    result.response.content.copyAndClose(channel)
                }
            })
        } else {
            call.respondText("No target endpoint is configured for this path", status = HttpStatusCode.NotFound)
        }

    }
}