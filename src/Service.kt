package com.adamringhede.kateway

data class Service(
    val name: String,
    val active: Boolean = true,
    val path: String,
    val targets: List<ServiceTarget>,
    val hosts: List<String> = emptyList(),
    val methods: List<String> = emptyList(),
    val public: Boolean = false,
    val retry: ServiceRetryConfig? = null
)

data class ServiceRetryConfig(
    val endpoints: List<ServiceRetryEndpoint>
) {
    fun shouldRetry(path: String, method: String) = endpoints.any { it.matches(path, method) }
}

data class ServiceRetryEndpoint(
    val path: String?,
    val methods: List<String>
) {
    fun matches(path: String, method: String): Boolean {
        return (this.path == null || path.startsWith(this.path)) && methods.contains(method)
    }
}

data class ServiceTarget(
    val url: String
)
