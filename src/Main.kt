package com.adamringhede.kateway

import com.adamringhede.kateway.storage.EtcdServicesRepo
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

    val etcdClient = Client.builder().endpoints("http://localhost:2379").build()
    val servicesRepo = EtcdServicesRepo(etcdClient)

    // TODO ports should be configurable
    embeddedServer(Netty, 8081) {
        adminModule(servicesRepo = servicesRepo)
    }.start(wait = false)

    embeddedServer(Netty, 8080) {
        proxyModule(servicesRepo = servicesRepo)
    }.start(wait = true)

}
