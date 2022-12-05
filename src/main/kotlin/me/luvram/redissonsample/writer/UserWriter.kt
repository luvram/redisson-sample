package me.luvram.redissonsample.writer

import com.fasterxml.jackson.databind.ObjectMapper
import me.luvram.redissonsample.User
import me.luvram.redissonsample.batch.RedissonBatchManager
import org.apache.juli.logging.LogFactory
import org.redisson.api.RMap
import org.redisson.api.RMapAsync
import org.redisson.api.RedissonClient
import org.redisson.codec.TypedJsonJacksonCodec
import org.springframework.stereotype.Component

@Component
class UserWriter(
    private val client: RedissonClient,
    private val batchManager: RedissonBatchManager,
    objectMapper: ObjectMapper
) {

    private val codec = TypedJsonJacksonCodec(String::class.java, User::class.java, objectMapper)

    companion object {
        val log = LogFactory.getLog(this.javaClass)
        private const val USER = "USER"
    }


    fun getCache(): RMap<String, User> {
        return client.getMap(USER, codec)
    }

    fun getBatchCache(): RMapAsync<String, User> {
        val batch = batchManager.getCurrentBatch()
        return batch.getMap<String, User>(USER, codec)
    }
}
