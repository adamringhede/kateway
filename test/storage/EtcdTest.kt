package com.adamringhede.apigateway.storage

import com.adamringhede.apigateway.Service
import io.etcd.jetcd.Client
import org.junit.Test
import kotlin.test.assertEquals

class EtcdTest {
    private val etcdClient = Client.builder().endpoints("http://localhost:2379").build()
    private val servicesRepo = EtcdServicesRepo(etcdClient)

    @Test
    fun `test manage services`() {
        servicesRepo.removeAll()
        assertEquals(0, servicesRepo.findAll().size)

        servicesRepo.insert(Service(name = "test", path = "/test", targets = emptyList()))
        assertEquals(1, servicesRepo.findAll().size)

        servicesRepo.insert(Service(name = "tes2", path = "/test", targets = emptyList()))
        servicesRepo.insert(Service(name = "tes2", path = "/test3", targets = emptyList()))
        assertEquals(2, servicesRepo.findAll().size)

        servicesRepo.removeAll()
        assertEquals(0, servicesRepo.findAll().size)
    }


}

