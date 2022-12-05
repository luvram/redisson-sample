package me.luvram.redissonsample.batch

import me.luvram.redissonsample.User
import me.luvram.redissonsample.writer.HeartbeatWriter
import me.luvram.redissonsample.writer.UserWriter
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be null`
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
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
    @Test
    fun `runMulti should success if input is single lock`() {
        val user = User(UUID.randomUUID().toString(), "name1", 1)
        val userCache = userWriter.getCache()
        batch.runMulti(userCache.getLock(user.name)) { batch ->
            val userBatchCache = userWriter.getBatchCache()
            userBatchCache.putAsync(user.id, user)
        }

        userCache[user.id].`should not be null`()
    }

    @Test
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
    }

    @Test
    fun `runMulti should discard if input block throws error`() {
        // given
        // when
        // then
    }

    @Test
    fun `runMulti should doCleanupAfterCompletion if input block return and jumps`() {
        // given
        // when
        // then
    }

    @Test
    fun `runMulti should use same batch in other runMulti`() {
        // given
        // when
        // then
    }

    @Test
    fun `runMulti should be atomic while another runMulti i run on the other thread`() {
        // given
        // when
        // then
    }
}
