package com.adamringhede.kateway

import com.adamringhede.kateway.storage.EtcdServicesRepo
import com.adamringhede.kateway.storage.MockServicesRepo
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.features.*
import io.ktor.routing.*
import io.ktor.http.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.etcd.jetcd.Client
import io.ktor.jackson.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>): Unit = Kateway().main(args)

class Kateway : CliktCommand() {
    val etcd: String? by option(
        help = "URL endpoints to Etcd for storing service configurations",
        envvar = "KATEWAY_ETCD_ENDPOINTS"
    )

    val adminPort: Int by option(
        help = "Port for HTTP API to administer the gateway",
        envvar = "KATEWAY_ADMIN_PORT"
    ).int().default(8081)

    val proxyPort: Int by option(
        help = "Port for traffic to go through the gatewa.",
        envvar = "KATEWAY_ADMIN_PORT"
    ).int().default(8080)

    override fun run() {

        val servicesRepo = when {
            etcd != null -> {
                println("Using Etcd at $etcd for gateway configurations")
                val etcdClient = Client.builder().endpoints(etcd).build()
                EtcdServicesRepo(etcdClient)
            }
            else -> MockServicesRepo()
        }

        embeddedServer(Netty, adminPort) {
            adminModule(servicesRepo = servicesRepo)
        }.start(wait = false)

        embeddedServer(Netty, proxyPort) {
            proxyModule(servicesRepo = servicesRepo)
        }.start(wait = true)
    }

}