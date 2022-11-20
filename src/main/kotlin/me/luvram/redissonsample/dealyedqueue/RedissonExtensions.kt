package me.luvram.redissonsample

import me.luvram.redissonsample.dealyedqueue.CustomDelayedQueue
import org.redisson.Redisson

fun <V> Redisson.getCustomDelayedQueue(queueName: String): CustomDelayedQueue<V> {
    return CustomDelayedQueue(this.commandExecutor, queueName)
}

