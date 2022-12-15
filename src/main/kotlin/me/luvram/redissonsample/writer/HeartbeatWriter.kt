package me.luvram.redissonsample.writer

import com.fasterxml.jackson.databind.ObjectMapper
import me.luvram.redissonsample.User
import me.luvram.redissonsample.batch.RedissonBatchManager
import me.luvram.redissonsample.springtransaction.CustomTransactionManager
import org.apache.juli.logging.LogFactory
import org.redisson.api.RLock
import org.redisson.api.RScoredSortedSet
import org.redisson.api.RScoredSortedSetAsync
import org.redisson.api.RedissonClient
import org.redisson.spring.transaction.RedissonTransactionManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class HeartbeatWriter(
    private val client: RedissonClient,
    private val transactionManager: CustomTransactionManager,
) {

    companion object {
        private val log = LogFactory.getLog(this.javaClass)
        private const val USER_HEARTBEAT = "USER-HEARTBEAT"
        private const val USER_HEARTBEAT_STRING = "USER-HEARTBEAT-STRING"
    }

    fun setByCustomTransaction(user: User) = transactionManager.transaction { batch ->
        val cache = batch.getScoredSortedSet<String>(USER_HEARTBEAT)
        cache.addAsync(Instant.now().toEpochMilli().toDouble(), user.id)
    }

    fun getScore(userId: String): Double? {
        return client.getScoredSortedSet<String>(USER_HEARTBEAT).getScore(userId)
    }

    fun getCache(): RScoredSortedSet<String> {
        return client.getScoredSortedSet(USER_HEARTBEAT)
    }

    fun getBatchCache(): RScoredSortedSetAsync<String> {
        val batch = transactionManager.getCurrentTransaction()
        return batch.getScoredSortedSet(USER_HEARTBEAT)
    }
}
