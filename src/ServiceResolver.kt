package com.adamringhede.kateway

import com.adamringhede.kateway.storage.ServicesRepo
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
        return services.find { it.active && uri.startsWith(it.path, true) }
    }

}