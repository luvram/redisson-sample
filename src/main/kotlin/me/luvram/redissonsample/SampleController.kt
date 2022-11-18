package me.luvram.redissonsample

import org.apache.juli.logging.LogFactory
import org.redisson.spring.transaction.RedissonTransactionManager
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController
class SampleController(
    val customDelayedQueue: CustomDelayedQueue<String>,
    val transactionService: TransactionService
) {
    companion object {
        val log = LogFactory.getLog(this.javaClass)
    }
    @GetMapping("/put/{key}")
    fun put(@PathVariable key: String) {
        log.info("key: ${key}")
        customDelayedQueue.offer(key, 20, TimeUnit.SECONDS)
    }

    @GetMapping("/transaction/{key}")
    fun transaction(
        @PathVariable id: String,
        name: String,
        age: Int

    ) {
        transactionService.saveUser(User(id, name, age))
    }
}
