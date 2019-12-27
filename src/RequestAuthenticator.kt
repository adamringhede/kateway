package com.adamringhede.kateway

import com.google.common.cache.CacheBuilder
import io.ktor.application.ApplicationCall
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.request.ApplicationRequest
import java.util.concurrent.TimeUnit

private const val AUTHENTICATION_CACHE_EXPIRATION: Long = 60

abstract class RequestAuthenticator<P, R> {
    private val cache = CacheBuilder.newBuilder()
        .expireAfterWrite(AUTHENTICATION_CACHE_EXPIRATION, TimeUnit.SECONDS)
        .build<P, R>()

    /**
     * Override this function that will resolve the authenticated account and other parameters.
     */
    protected abstract fun authenticate(params: P): R?
    protected abstract fun identify(request: ApplicationRequest): P

    open fun modify(builder: HttpRequestBuilder, authResp: R): Unit {}

    fun test(request: ApplicationRequest): R? {
        val params = identify(request)
        val cachedResponse = cache.getIfPresent(params)
        if (cachedResponse != null) {
            return cachedResponse
        }
        val freshResponse = authenticate(params)
        if (freshResponse != null) {
            cache.put(params, freshResponse)
        }
        return freshResponse
    }

    fun testAndModify(request: ApplicationRequest, builder: HttpRequestBuilder) {
        test(request)?.let { modify(builder, it) }
    }
}

class MockRequestAuthenticator : RequestAuthenticator<Any, Unit>() {
    override fun authenticate(params: Any): Unit {}
    override fun identify(request: ApplicationRequest): Any {
        return "<mocked>"
    }
}