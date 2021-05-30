package com.example.redissonsample

import org.apache.juli.logging.LogFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController
class SampleController(
    val customDelayedQueue: CustomDelayedQueue<String>
) {
    companion object {
        val log = LogFactory.getLog(this.javaClass)
    }
    @GetMapping("/put/{key}")
    fun put(@PathVariable key: String) {
        log.info("key: ${key}")
        customDelayedQueue.offer(key, 20, TimeUnit.SECONDS)
    }
}