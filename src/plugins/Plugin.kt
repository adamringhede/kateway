package com.adamringhede.apigateway.plugins

import io.ktor.application.Application

interface Plugin {
    fun setup(app: Application)
}