package com.example.redissonsample

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
class RedissonSampleApplication

fun main(args: Array<String>) {
    runApplication<RedissonSampleApplication>(*args)
}
