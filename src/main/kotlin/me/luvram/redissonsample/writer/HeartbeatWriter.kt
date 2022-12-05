package me.luvram.redissonsample.writer

import com.fasterxml.jackson.databind.ObjectMapper
import me.luvram.redissonsample.User
import me.luvram.redissonsample.batch.RedissonBatchManager
import me.luvram.redissonsample.writer.UserWriter.Companion
import org.apache.juli.logging.LogFactory
import org.redisson.api.RMap
import org.redisson.api.RMapAsync
import org.redisson.api.RScoredSortedSet
import org.redisson.api.RScoredSortedSetAsync
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component

@Component
class HeartbeatWriter(
    private val client: RedissonClient,
    private val batchManager: RedissonBatchManager,
    objectMapper: ObjectMapper
){

    companion object {
        private val log = LogFactory.getLog(this.javaClass)
        private const val USER_HEARTBEAT = "USER-HEARTBEAT"
    }

    fun getCache(): RScoredSortedSet<String> {
        return client.getScoredSortedSet(USER_HEARTBEAT)
    }

    fun getBatchCache(): RScoredSortedSetAsync<String> {
        val batch = batchManager.getCurrentBatch()
        return batch.getScoredSortedSet(USER_HEARTBEAT)
    }
}
