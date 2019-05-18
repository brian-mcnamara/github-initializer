package dev.bmac.github

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import redis.embedded.RedisServer
import java.net.URI
import java.net.URL
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * Using embedded redis, maybe one day it will scale to require an actual redis instance
 */
@Component
@ConditionalOnProperty(name = ["redis.local"], havingValue = "true")
class LocalRedis(@Value("\${redis.url}") redisUrl: String) {
    private val uri = URI(redisUrl)
    private val redisServer = RedisServer(uri.port)

    @PostConstruct
    fun init() {
        redisServer.start()
    }

    @PreDestroy
    fun destroy() {
        redisServer.stop()
    }
}