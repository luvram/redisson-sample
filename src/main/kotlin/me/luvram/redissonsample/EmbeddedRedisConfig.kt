package me.luvram.redissonsample

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import redis.embedded.RedisServer
import java.io.IOException
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy


@Configuration
class EmbeddedRedisConfig {
    @Value("\${spring.redis.port}")
    private val redisPort = 0

    private var redisServer: RedisServer? = null

//    @PostConstruct
//    @Throws(IOException::class)
//    fun redisServer() {
//        redisServer = RedisServer.builder()
//            .port(redisPort)
//            .build()
//
//        redisServer!!.start()
//    }

//    @PreDestroy
//    fun stopRedis() {
//        if (redisServer != null) {
//            redisServer!!.stop()
//        }
//    }
}
