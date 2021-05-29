package com.example.redissonsample

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("spring.redis")
class RedisProperties(
    var host: String = "",
    var port: Int = 0
) {
}