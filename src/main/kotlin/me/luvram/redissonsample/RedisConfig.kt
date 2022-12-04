package me.luvram.redissonsample

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.luvram.redissonsample.batch.RedissonBatchManager
import me.luvram.redissonsample.dealyedqueue.CustomDelayedQueue
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.redisson.spring.transaction.RedissonTransactionManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement


@Configuration
@EnableRedisRepositories
@EnableTransactionManagement // transaction 활성화
class RedisConfig(
    val redisProperties: RedisProperties
) {

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory = LettuceConnectionFactory(redisProperties.host, redisProperties.port)

    @Bean(destroyMethod="shutdown")
    fun redissonClient(): RedissonClient {
        val config = Config().apply {
            this.useSingleServer().address = "redis://${redisProperties.host}:${redisProperties.port}"
        }
        return Redisson.create(config)
    }


    @Bean
    fun transactionManager(redisson: RedissonClient): RedissonTransactionManager {
        return RedissonTransactionManager(redisson)
    }

//    @Bean
//    fun transactionManager(redissonClient: RedissonClient): CustomTransactionManager {
//        return CustomTransactionManager(redissonClient)
//    }

    @Bean
    fun redissonBatchManager(redisson: RedissonClient): RedissonBatchManager {
        return RedissonBatchManager(redisson)
    }

    @Bean
    fun objectMapper(): ObjectMapper {
        return jacksonObjectMapper()
    }

    @Bean
    fun customDelayedQueue(redissonClient: RedissonClient): CustomDelayedQueue<String> {
        return (redissonClient as Redisson).getCustomDelayedQueue("deQueue")
    }
}
