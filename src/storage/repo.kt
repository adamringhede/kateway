package com.adamringhede.apigateway.storage

import com.adamringhede.apigateway.Service

interface ServicesRepo {
    fun findAll(): List<Service>
    fun insert(service: Service)
    fun removeAll()
}