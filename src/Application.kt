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
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>): Unit {

    // TODO ports should be configurable
    embeddedServer(Netty, 8081) {
        adminModule()
    }.start(wait = false)

    embeddedServer(Netty, 8080) {
        proxyModule()
    }.start(wait = true)

}

// TODO replace with dependency injection
private val etcdClient = Client.builder().endpoints("http://localhost:2379").build()
val servicesRepo = EtcdServicesRepo(etcdClient)
