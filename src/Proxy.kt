package com.adamringhede.kateway

import com.adamringhede.kateway.plugins.CompressionPlugin
import com.adamringhede.kateway.plugins.CorsPlugin
import com.adamringhede.kateway.plugins.Plugin
import com.adamringhede.kateway.storage.ServicesRepo
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.request.*
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.util.filter
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.copyAndClose
import java.time.Duration
import java.util.concurrent.TimeUnit

internal const val MAX_RETRY_COUNT: Int = 5
private val filteredProxyHeaders = arrayOf(HttpHeaders.ContentType, HttpHeaders.ContentLength, HttpHeaders.TransferEncoding).map { it.toLowerCase() }

private val plugins: List<Plugin> = listOf(
    CompressionPlugin(),
    CorsPlugin()
)

fun Application.proxyModule(servicesRepo: ServicesRepo, requestAuthenticator: RequestAuthenticator<*, *>? = null) {

    plugins.forEach { it.setup(this) }

    val serviceResolver = ServiceResolver(servicesRepo)
    val proxy = Proxy(requestAuthenticator)

    intercept(ApplicationCallPipeline.Call) {
        val service = serviceResolver.find(call.request.uri)
        val target = service?.targets?.firstOrNull()

        if (service != null && target != null) {
            proxy.authenticateCall(call, service)
            proxy.proxyCall(call, service, target)
        } else {
            call.respondText("No target endpoint is configured for this path", status = HttpStatusCode.NotFound)
        }
    }
}

internal class Proxy(private val authenticator: RequestAuthenticator<*, *>? = null) {
    private val circuitBreakers = mutableMapOf<String, CircuitBreaker>()

    private fun getCircuitBreaker(service: Service): CircuitBreaker {
        if (!circuitBreakers.containsKey(service.name)) {
            circuitBreakers.putIfAbsent(service.name, CircuitBreaker.ofDefaults(service.name))
        }
        return circuitBreakers[service.name]!!
    }

    suspend fun authenticateCall(call: ApplicationCall, service: Service): Any? {
        if (!service.public) {
            if (authenticator == null) {
                call.respond(HttpStatusCode.Unauthorized, "Authentication not supported")
                return null
            }
        } else {
            return null
        }
        val resp = authenticator.test(call.request)
        if (resp == null) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid authentication credentials")
            return null
        }
        return resp
    }

    suspend fun proxyCall(call: ApplicationCall, service: Service, target: ServiceTarget) {
        val readChannel = call.receiveChannel()
        val requestBuilder = HttpRequestBuilder().apply {
            url.takeFrom(target.url.trimEnd('/') + "/" + call.request.path().trimStart('/'))
            url.parameters.appendAll(call.request.queryParameters)

            method = call.request.httpMethod
            headers.appendAll(call.request.headers.filter { key, _ -> !key.equals(HttpHeaders.ContentType, true) })
            headers.append(HttpHeaders.XForwardedFor, call.request.local.remoteHost)

            body = object : OutgoingContent.ReadChannelContent() {
                override val contentType: ContentType? = call.request.contentType()
                override fun readFrom() = readChannel
            }
        }

        authenticator?.testAndModify(call.request, requestBuilder)

        val retry = Retry.of(service.name, createRetryConfig(call, service))
        val result = retry.executeSuspendFunction {
            executeRequest(requestBuilder, getCircuitBreaker(service))
        }

        val proxiedHeaders = result.response.headers
        val contentType = proxiedHeaders[HttpHeaders.ContentType]
        val contentLength = proxiedHeaders[HttpHeaders.ContentLength]

        call.respond(object : OutgoingContent.WriteChannelContent() {
            override val contentLength: Long? = contentLength?.toLong()
            override val contentType: ContentType? = contentType?.let { ContentType.parse(it) }
            override val headers: Headers = Headers.build {
                appendAll(proxiedHeaders.filter { key, _ ->
                    !filteredProxyHeaders.contains(key.toLowerCase())
                })
            }
            override val status: HttpStatusCode? = result.response.status
            override suspend fun writeTo(channel: ByteWriteChannel) {
                result.response.content.copyAndClose(channel)
            }
        })
    }

    private fun createRetryConfig(call: ApplicationCall, service: Service): RetryConfig {
        val shouldRetry = service.retry?.shouldRetry(call.request.path(), call.request.httpMethod.value) == true
        return RetryConfig.custom<HttpClientCall>()
            .maxAttempts(if (shouldRetry) MAX_RETRY_COUNT else 1)
            .waitDuration(Duration.ofMillis(1000))
            .retryOnResult { it.response.status.value >= 500 } // Including internal server error
            .build()
    }

    private suspend fun executeRequest(requestBuilder: HttpRequestBuilder, circuitBreaker: CircuitBreaker): HttpClientCall {
        // TODO Implement caching
        // TODO Implement monitoring
        val client= HttpClient(Apache)
        val start = System.nanoTime()
        val result = circuitBreaker.executeSuspendFunction { client.execute(requestBuilder) }
        val duration = System.nanoTime() - start
        if (result.response.status.value > 500) { // Excluding internal server error
            circuitBreaker.onError(duration, TimeUnit.NANOSECONDS, UnknownError())
        }
        return result
    }
}

