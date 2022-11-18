package me.luvram.redissonsample

import org.redisson.Redisson

fun <V> Redisson.getCustomDelayedQueue(queueName: String): CustomDelayedQueue<V> {
    return CustomDelayedQueue(this.commandExecutor, queueName)
}

