package com.adamringhede.apigateway

import com.adamringhede.apigateway.storage.ServicesRepo
import java.util.*
import kotlin.concurrent.timerTask

class ServiceResolver(private val servicesRepo: ServicesRepo) {
    private var services = servicesRepo.findAll()
    val updateFrequency = 10_000L

    private fun sync() {
        this.services = servicesRepo.findAll()
    }

    init {
        val timer = Timer()
        timer.scheduleAtFixedRate(timerTask { sync() }, updateFrequency, updateFrequency)
    }

    fun find(uri: String): Service? {
        return services.find { uri.startsWith(it.path, true) }
    }

}