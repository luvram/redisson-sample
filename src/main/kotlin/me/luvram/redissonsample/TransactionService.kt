package me.luvram.redissonsample

import com.fasterxml.jackson.databind.ObjectMapper
import me.luvram.redissonsample.batch.RedissonBatchManager
import org.apache.juli.logging.LogFactory
import org.redisson.api.BatchOptions
import org.redisson.api.BatchOptions.ExecutionMode
import org.redisson.api.RMap
import org.redisson.api.RTransaction
import org.redisson.api.RedissonClient
import org.redisson.api.TransactionOptions
import org.redisson.codec.TypedJsonJacksonCodec
import org.redisson.spring.transaction.RedissonTransactionManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.concurrent.TimeUnit


@Service
class TransactionService(
    val batchManager: RedissonBatchManager,
    val client: RedissonClient,
    val objectMapper: ObjectMapper
) {
    companion object {
        val log = LogFactory.getLog(this.javaClass)
        private const val USER = "USER"
    }

    private val codec = TypedJsonJacksonCodec(User::class.java, User::class.java, objectMapper)

    fun saveUserTransaction(user: User) {
        val transaction: RTransaction = client.createTransaction(TransactionOptions.defaults())

        val userCache: RMap<String, User> = transaction.getMap("USER", codec)
        val heartbeat = transaction.getSet<String>("USER-HEARTBEAT-STRING")

        userCache[user.id] = user
        heartbeat.add(user.id)

        transaction.commit()
    }

    fun saveUserBatch(user: User) {

        val batchOption = BatchOptions.defaults().executionMode(ExecutionMode.REDIS_WRITE_ATOMIC)
        val batch = client.createBatch(batchOption)

        val userCache = batch.getMap<String, User>(USER, codec)
        val heartbeat = batch.getScoredSortedSet<String>("USER-HEARTBEAT-BATCH")

        userCache.putAsync(user.id, user)
        heartbeat.addAsync(Instant.now().toEpochMilli().toDouble(), user.id)

        val res = batch.execute()

        res.responses
    }

    fun saveUserMultiLock(user: User) {
        val userCache: RMap<String, User> = client.getMap(USER, codec)
        val heartbeat = client.getSet<String>("USER-HEARTBEAT-STRING")

        val userLock = userCache.getLock(user.id)
        val heartbeatLock = heartbeat.getLock(user.id)


        val multiLock = client.getMultiLock(userLock, heartbeatLock)


        if (multiLock.tryLock(1000, TimeUnit.MILLISECONDS)) {
            try {
                userCache[user.id] = user
                heartbeat.add(user.id)
            } finally {
                log.debug { "unlock" }
                multiLock.unlock()
            }
        } else {
            log.warn { "Fail to get lock" }
        }
    }

    fun saveUserBatchManager(user: User) = batchManager.runMulti(client.getLock("user.${user.id}")) { batch ->
        val userCache = batch.getMap<String, User>(USER, codec)
        userCache.putAsync(user.id, user)
        saveUserHeartbeat(user)
    }

    fun saveUserHeartbeat(user: User) {
        val batch = batchManager.getCurrentBatch()
        val heartbeat = batch.getScoredSortedSet<String>("USER-HEARTBEAT-BATCH")
        heartbeat.addAsync(Instant.now().toEpochMilli().toDouble(), user.id)
    }
}
