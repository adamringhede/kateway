package com.adamringhede.kateway

data class Service(
    val name: String,
    val active: Boolean = true,
    val path: String,
    val targets: List<ServiceTarget>,
    val hosts: List<String> = emptyList(),
    val methods: List<String> = emptyList()
)

data class ServiceTarget(
    val url: String
)
