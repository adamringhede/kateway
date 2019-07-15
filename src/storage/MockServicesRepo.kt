package com.adamringhede.kateway.storage

import com.adamringhede.kateway.Service

class MockServicesRepo : ServicesRepo {
    private val services = mutableMapOf<String, Service>()

    override fun findAll(): List<Service> {
        return services.values.toList()
    }

    override fun insert(service: Service) {
        services[service.name] = service
    }

    override fun remove(name: String) {
        services.remove(name)
    }

    override fun removeAll() {
        services.clear()
    }

}