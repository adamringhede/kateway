package com.adamringhede.kateway.storage

import com.adamringhede.kateway.Service

interface ServicesRepo {
    fun findAll(): List<Service>
    fun insert(service: Service)
    fun removeAll()
}