package com.adamringhede.kateway.plugins

import io.ktor.application.Application

interface Plugin {
    fun setup(app: Application)
}