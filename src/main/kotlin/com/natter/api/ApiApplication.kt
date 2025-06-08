package com.natter.api

import com.natter.api.config.AppConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackageClasses = [AppConfiguration::class]) class ApiApplication

fun main(args: Array<String>) {
    runApplication<ApiApplication>(*args)
}
