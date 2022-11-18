package me.luvram.redissonsample

import org.redisson.QueueTransferService
import org.redisson.QueueTransferTask
import org.redisson.RedissonObject
import org.redisson.RedissonTopic
import org.redisson.api.RFuture
import org.redisson.api.RTopic
import org.redisson.client.codec.LongCodec
import org.redisson.client.protocol.RedisCommands
import org.redisson.command.CommandAsyncExecutor
import java.util.concurrent.TimeUnit

class CustomDelayedQueue<V>(
    commandExecutor: CommandAsyncExecutor,
    name: String
) : RedissonObject(commandExecutor.connectionManager.codec, commandExecutor, name) {

    private val queueTransferService: QueueTransferService = QueueTransferService()
    private var channelName: String = prefixName("redisson_delay_queue_channel", getName())
    private var timeoutSetName: String = prefixName("redisson_delay_queue_timeout", getName())

    init {
        val task: QueueTransferTask = object : QueueTransferTask(commandExecutor.connectionManager) {
            override fun pushTaskAsync(): RFuture<Long?> {
                return commandExecutor.evalWriteAsync(
                    getName(), LongCodec.INSTANCE, RedisCommands.EVAL_LONG,
                    "local expiredValues = redis.call('zrangebyscore', KEYS[2], 0, ARGV[1], 'limit', 0, ARGV[2]); "
                            + "if #expiredValues > 0 then "
                                + "for i, v in ipairs(expiredValues) do "
                                    + "redis.call('rpush', KEYS[1], v);"
                                + "end; "
                                + "redis.call('zrem', KEYS[2], unpack(expiredValues));"
                            + "end; " // get startTime from scheduler queue head task
                            + "local v = redis.call('zrange', KEYS[2], 0, 0, 'WITHSCORES'); "
                            + "if v[1] ~= nil then "
                                + "return v[2]; "
                            + "end "
                            + "return nil;",
                    listOf<Any>(getName(), timeoutSetName),
                    System.currentTimeMillis(), 100
                )
            }

            override fun getTopic(): RTopic {
                return RedissonTopic.createRaw(LongCodec.INSTANCE, commandExecutor, channelName)
            }
        }

        queueTransferService.schedule(timeoutSetName, task)
    }

    fun offer(e: V, delay: Long, timeUnit: TimeUnit) {
        get(offerAsync(e, delay, timeUnit))
    }

    private fun offerAsync(e: V, delay: Long, timeUnit: TimeUnit): RFuture<Void> {
        if (delay < 0) {
            throw IllegalArgumentException("Delay can't be negative")
        }
        val delayInMs = timeUnit.toMillis(delay)
        val timeout = System.currentTimeMillis() + delayInMs
        return commandExecutor.evalWriteAsync(
            rawName, codec, RedisCommands.EVAL_VOID,
            "redis.call('zadd', KEYS[2], ARGV[1], ARGV[2]);"
                    // to all scheduler workers
                    + "local v = redis.call('zrange', KEYS[2], 0, 0); "
                    + "if v[1] == ARGV[2] then "
                    + "redis.call('publish', KEYS[3], ARGV[1]); "
                    + "end;",
            listOf<Any>(rawName, timeoutSetName, channelName),
            timeout, encode(e)
        )
    }
}
