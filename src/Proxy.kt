package com.adamringhede.apigateway

import com.adamringhede.apigateway.plugins.CompressionPlugin
import com.adamringhede.apigateway.plugins.CorsPlugin
import com.adamringhede.apigateway.plugins.Plugin
import com.adamringhede.apigateway.storage.ServicesRepo
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.jackson.jackson
import io.ktor.request.authorization
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

fun Application.addPlugin(plugin: Plugin) = plugin.setup(this)

fun Application.proxyModule(testing: Boolean = false, servicesRepo: ServicesRepo) {

    addPlugin(CompressionPlugin())
    addPlugin(CorsPlugin())

    val serviceResolver = ServiceResolver(servicesRepo)

    val client= HttpClient(Apache)

    intercept(ApplicationCallPipeline.Call) {
        val service = serviceResolver.find(call.request.uri)
        val target = service?.targets?.firstOrNull()

        if (service != null && target != null) {
            val requestBuilder = HttpRequestBuilder().apply {
                url.takeFrom(target.url)
                url.parameters.appendAll(call.request.queryParameters)
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