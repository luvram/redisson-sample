package me.luvram.redissonsample.springtransaction

import me.luvram.redissonsample.User
import me.luvram.redissonsample.writer.HeartbeatWriter
import me.luvram.redissonsample.writer.UserWriter
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be null`
import org.amshove.kluent.`should not be null`
import org.amshove.kluent.`should throw`
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.NoTransactionException
import java.time.Instant
import java.util.UUID

@ActiveProfiles("develop", "test")
@SpringBootTest
class CustomTransactionManagerTest @Autowired constructor(
    private val customTransactionManager: CustomTransactionManager,

    private val userWriter: UserWriter,
    private val heartbeatWriter: HeartbeatWriter
) {

    @Test
    fun `transaction should success if input is single lock`() {
        val user = User(UUID.randomUUID().toString(), "name1", 1)
        val userLock = userWriter.getLock(user.name)
        customTransactionManager.transaction(userLock) { batch ->
            val userBatchCache = userWriter.getBatchCache()
            userBatchCache.putAsync(user.id, user)
        }

        userWriter.get(user.id).`should not be null`()
        invoking { customTransactionManager.getCurrentTransaction() } `should throw` NoTransactionException::class
    }

    @Test
    fun `transaction should success if input is mult lock`() {
        val user = User(UUID.randomUUID().toString(), "name2", 2)
        val userLock = userWriter.getLock(user.name)
        customTransactionManager.transaction(userLock) { batch ->
            userWriter.setByCustomTransaction(user)
            heartbeatWriter.setByCustomTransaction(user)
        }

        userWriter.get(user.id).`should not be null`()
        heartbeatWriter.getScore(user.id).`should not be null`()

        invoking { customTransactionManager.getCurrentTransaction() } `should throw` NoTransactionException::class

    }

    @Test
    fun `transactrion should discard if input block throws error`() {
        val user = User(UUID.randomUUID().toString(), "name2", 2)
        val userLock = userWriter.getLock(user.name)
        invoking {
            customTransactionManager.transaction(userLock) { batch ->
                userWriter.setByCustomTransaction(user)
                heartbeatWriter.setByCustomTransaction(user)
                throw Exception()
            }
        } `should throw` Exception::class

        userWriter.get(user.id).`should be null`()
        heartbeatWriter.getScore(user.id).`should be null`()
        invoking { customTransactionManager.getCurrentTransaction() } `should throw` NoTransactionException::class

    }

    @Test
    fun `runMulti should use same batch in nested runMulti`() {
        val user = User(UUID.randomUUID().toString(), "name2", 2)
        val userLock = userWriter.getLock(user.name)

        customTransactionManager.transaction(userLock) {
            val batch1 = customTransactionManager.getCurrentTransaction()
            customTransactionManager.transaction { batch2 ->
                val batch2 = customTransactionManager.getCurrentTransaction()

                batch1.`should be equal to`(batch2)
            }
        }

        userWriter.setWithHeartbeatByCustomTransaction(user)
        userWriter.get(user.id).`should not be null`()
        heartbeatWriter.getScore(user.id).`should not be null`()
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    @RepeatedTest(1000)
    fun `runMulti should be atomic while another runMulti is run on the other thread`() {
        val user = User(UUID.randomUUID().toString(), "name2", 2)
        userWriter.setWithHeartbeatByCustomTransaction(user)
        userWriter.get(user.id).`should not be null`()
        heartbeatWriter.getScore(user.id).`should not be null`()

    }

}
