package dev.bmac.github

import org.springframework.stereotype.Component
import redis.embedded.RedisServer
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * Using embedded redis, maybe one day it will scale to require an actual redis instance
 */
@Component
class LocalRedis {
    val redisServer = RedisServer(6890)

    @PostConstruct
    fun init() {
        redisServer.start()
    }

    @PreDestroy
    fun destroy() {
        redisServer.stop()
    }
}