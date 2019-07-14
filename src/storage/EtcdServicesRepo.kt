package com.adamringhede.kateway.storage

import com.adamringhede.kateway.Service
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.etcd.jetcd.ByteSequence
import io.etcd.jetcd.Client
import io.etcd.jetcd.options.DeleteOption
import io.etcd.jetcd.options.GetOption
import java.nio.charset.Charset

class EtcdServicesRepo(private val client: Client) : ServicesRepo {
    private val charset = Charset.defaultCharset()
    private val jsonMapper = ObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        registerModule(KotlinModule())
    }
    private fun withPrefix(prefix: String) = GetOption.newBuilder().withPrefix(key(prefix)).build()
    private fun withDeletePrefix(prefix: String) = DeleteOption.newBuilder().withPrefix(key(prefix)).build()

    override fun findAll(): List<Service> {
        return client.kvClient.get(key(""), withPrefix("services")).get().kvs
            .map { it.value.toString(charset) }
            .map { jsonMapper.readValue(it, Service::class.java) }
    }

    override fun insert(service: Service) {
        client.kvClient.put(key("services.${service.name}"), toBytes(service)).get()
    }

    override fun removeAll() {
        client.kvClient.delete(key(""), withDeletePrefix("services")).get()
    }

    private fun key(path: String) =
        ByteSequence.from("kateway.$path", charset)

    private fun toBytes(service: Service) =
        ByteSequence.from(jsonMapper.writeValueAsBytes(service))

}