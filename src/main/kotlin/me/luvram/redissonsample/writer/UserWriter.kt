package me.luvram.redissonsample.writer

import com.fasterxml.jackson.databind.ObjectMapper
import me.luvram.redissonsample.User
import me.luvram.redissonsample.batch.RedissonBatchManager
import me.luvram.redissonsample.springtransaction.CustomTransactionManager
import org.apache.juli.logging.LogFactory
import org.redisson.api.RLock
import org.redisson.api.RMap
import org.redisson.api.RMapAsync
import org.redisson.api.RedissonClient
import org.redisson.codec.TypedJsonJacksonCodec
import org.redisson.spring.transaction.RedissonTransactionManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserWriter(
    private val client: RedissonClient,
    private val transactionManager: CustomTransactionManager,
    private val heartbeatWriter: HeartbeatWriter,
    objectMapper: ObjectMapper
) {

    private val codec = TypedJsonJacksonCodec(String::class.java, User::class.java, objectMapper)

    companion object {
        val log = LogFactory.getLog(this.javaClass)
        private const val USER = "USER"
    }

    fun setWithHeartbeatByCustomTransaction(user: User) = transactionManager.transaction(getLock(user.name)) { batch ->
        val cache = batch.getMap<String, User>(USER, codec)
        cache.putAsync(user.id, user)
        heartbeatWriter.setByCustomTransaction(user)
    }

    fun setByCustomTransaction(user: User) = transactionManager.transaction { batch ->
        val cache = batch.getMap<String, User>(USER, codec)
        cache.putAsync(user.id, user)
    }

    fun getLock(lockName: String): RLock {
        return client.getMap<String, User>(USER, codec).getLock(lockName)
    }

    fun get(userId: String): User? {
        return client.getMap<String, User>(USER, codec)[userId]
    }

    fun getCache(): RMap<String, User> {
        return client.getMap(USER, codec)
    }

    fun getBatchCache(): RMapAsync<String, User> {
        val batch = transactionManager.getCurrentTransaction()
        return batch.getMap(USER, codec)
    }
}
