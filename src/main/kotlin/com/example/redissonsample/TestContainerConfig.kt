package com.example.redissonsample

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.testcontainers.utility.DockerImageName
import javax.annotation.PostConstruct

@Profile("local")
@Configuration
class TestContainerConfig {
    @PostConstruct
    fun runRedisServer() {
        RedisTestContainer(DockerImageName.parse("redis"))
    }

}