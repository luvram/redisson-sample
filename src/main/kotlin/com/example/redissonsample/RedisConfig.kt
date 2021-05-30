package com.example.redissonsample

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.redisson.QueueTransferService
import org.redisson.Redisson
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.redisson.codec.TypedJsonJacksonCodec
import org.redisson.config.Config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories


@Configuration
@EnableRedisRepositories
class RedisConfig(
    val redisProperties: RedisProperties
) {

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory = LettuceConnectionFactory(redisProperties.host, redisProperties.port)

    @Bean
    fun redissonClient(): RedissonClient {
        val config = Config().apply {
            this.useSingleServer().address = "redis://${redisProperties.host}:${redisProperties.port}"
        }
        return Redisson.create(config)
    }

    @Bean
    fun objectMapper(): ObjectMapper {
        return jacksonObjectMapper()
    }

    @Bean
    fun customDelayedQueue(redissonClient: RedissonClient): CustomDelayedQueue<String> {
        return (redissonClient as Redisson).getCustomDelayedQueue("deQueue")
    }

    @Bean
    fun userCache(redissonClient: RedissonClient, objectMapper: ObjectMapper): RMap<String, User> {
        return redissonClient.getMap("TEST", TypedJsonJacksonCodec(User::class.java, User::class.java, objectMapper))
    }

}