package com.adamringhede.kateway

import com.adamringhede.kateway.storage.EtcdServicesRepo
import com.adamringhede.kateway.storage.MockServicesRepo
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.etcd.jetcd.Client
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

fun main(args: Array<String>): Unit = Kateway().main(args)

class Kateway : CliktCommand() {
    private val etcd: String? by option(
        help = "URL endpoints to Etcd for storing service configurations",
        envvar = "KATEWAY_ETCD_ENDPOINTS"
    )

    private val adminPort: Int by option(
        help = "Port for HTTP API to administer the gateway",
        envvar = "KATEWAY_ADMIN_PORT"
    ).int().default(8081)

    private val proxyPort: Int by option(
        help = "Port for traffic to go through the gateway.",
        envvar = "KATEWAY_PROXY_PORT"
    ).int().default(8080)

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run() {

        val servicesRepo = when {
            etcd != null -> {
                logger.info("Using Etcd at $etcd for gateway configurations")
                val etcdClient = Client.builder().endpoints(etcd).build()
                EtcdServicesRepo(etcdClient)
            }
            else -> {
                logger.warn("Using an in-memory storage for gateway configurations. Changes will not be persisted.")
                MockServicesRepo()
            }
        }

        embeddedServer(Netty, adminPort) {
            adminModule(servicesRepo = servicesRepo)
        }.start(wait = false)

        embeddedServer(Netty, proxyPort) {
            proxyModule(servicesRepo = servicesRepo)
        }.start(wait = true)
    }

}