package com.natter.api.config

import com.natter.api.controller.SpaceController
import com.natter.api.core.DatabaseService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackageClasses = [
    SpaceController::class
])
class AppConfiguration {
    
    @Value("\${spring.datasource.url}")
    private lateinit var url: String

    @Value("\${spring.datasource.username}")
    private lateinit var username: String

    @Value("\${spring.datasource.password}")
    private lateinit var password: String

    @Bean
    fun databaseService(): DatabaseService {
        println("HELLO")
        return DatabaseService(url, username, password)
    }
}