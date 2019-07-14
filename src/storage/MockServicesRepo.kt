package com.adamringhede.apigateway.storage

import com.adamringhede.apigateway.Service

class MockServicesRepo : ServicesRepo {
    private val services = mutableMapOf<String, Service>()

    override fun findAll(): List<Service> {
        return services.values.toList()
    }

    override fun insert(service: Service) {
        services[service.name] = service
    }

    override fun removeAll() {
        services.clear()
    }

}