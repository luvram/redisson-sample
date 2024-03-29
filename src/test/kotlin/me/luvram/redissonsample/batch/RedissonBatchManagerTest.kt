package me.luvram.redissonsample.batch

import me.luvram.redissonsample.User
import me.luvram.redissonsample.writer.HeartbeatWriter
import me.luvram.redissonsample.writer.UserWriter
import org.amshove.kluent.invoking
import org.amshove.kluent.should
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be null`
import org.amshove.kluent.`should not be null`
import org.amshove.kluent.`should throw`
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.NoTransactionException
import java.time.Instant
import java.util.Random
import java.util.UUID

@ActiveProfiles("develop", "test")
@SpringBootTest
internal class RedissonBatchManagerTest @Autowired constructor(
    private val client: RedissonClient,
    private val batch: RedissonBatchManager,
    private val userWriter: UserWriter,
    private val heartbeatWriter: HeartbeatWriter
) {
//    @Test
    fun `runMulti should success if input is single lock`() {
        val user = User(UUID.randomUUID().toString(), "name1", 1)
        val userCache = userWriter.getCache()
        batch.runMulti(userCache.getLock(user.name)) { batch ->
            val userBatchCache = userWriter.getBatchCache()
            userBatchCache.putAsync(user.id, user)
        }

        userCache[user.id].`should not be null`()
        invoking { batch.getCurrentBatch() } `should throw` NoTransactionException::class
    }

//    @Test
    fun `runMulti should success if input is mult lock`() {
        val user = User(UUID.randomUUID().toString(), "name2", 2)
        val userCache = userWriter.getCache()
        val heartbeatCache = heartbeatWriter.getCache()
        batch.runMulti(userCache.getLock(user.name)) { batch ->
            val userBatchCache = userWriter.getBatchCache()
            val heartbeatBatchCache = heartbeatWriter.getBatchCache()
            userBatchCache.putAsync(user.id, user)
            heartbeatBatchCache.addAsync(Instant.now().toEpochMilli().toDouble(), user.id)
        }

        userCache[user.id].`should not be null`()
        heartbeatCache.getScore(user.id).`should not be null`()
        invoking { batch.getCurrentBatch() } `should throw` NoTransactionException::class
    }

//    @Test
    fun `runMulti should discard if input block throws error`() {
        val user = User(UUID.randomUUID().toString(), "name2", 2)
        val userCache = userWriter.getCache()
        val heartbeatCache = heartbeatWriter.getCache()
        invoking {
            batch.runMulti(userCache.getLock(user.name)) { batch ->
                val userBatchCache = userWriter.getBatchCache()
                val heartbeatBatchCache = heartbeatWriter.getBatchCache()
                userBatchCache.putAsync(user.id, user)
                heartbeatBatchCache.addAsync(Instant.now().toEpochMilli().toDouble(), user.id)
                throw Exception()
            }
        } `should throw` Exception::class

        userCache[user.id].`should be null`()
        heartbeatCache.getScore(user.id).`should be null`()
        invoking { batch.getCurrentBatch() } `should throw` NoTransactionException::class
    }

//    @Test
    fun `runMulti should use same batch in nested runMulti`() {
        val user = User(UUID.randomUUID().toString(), "name2", 2)
        val userCache = userWriter.getCache()

        batch.runMulti(userCache.getLock(user.name)) {
            val batch1 = batch.getCurrentBatch()

            batch.runMulti { batch2 ->
                val batch2 = batch.getCurrentBatch()

                batch1.`should be equal to`(batch2)
                // it'll hard to implement to re-use transaction logic
            }
        }
    }

    @Test
    fun `runMulti should be atomic while another runMulti i run on the other thread`() {
        // given
        // when
        // then
    }
}
