package dev.bmac.github.storage

import io.lettuce.core.RedisClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import redis.embedded.RedisServer
import java.net.ServerSocket
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

interface Redis {
    fun getClient() : RedisClient
}

/**
 * Using embedded redis, maybe one day it will scale to require an actual redis instance
 */
@Component
@ConditionalOnProperty(name = ["redis.local"], havingValue = "true")
class LocalRedis() : Redis {
    private lateinit var redisServer : RedisServer

    init {
        val port = ServerSocket(0).use {
            it.localPort
        }
        redisServer = RedisServer(port)
    }

    @PostConstruct
    fun init() {
        redisServer.start()
    }

    @PreDestroy
    fun destroy() {
        redisServer.stop()
    }

    override fun getClient(): RedisClient {
        return RedisClient.create("redis://localhost:${redisServer.ports()[0]}")
    }
}

@Component
@ConditionalOnMissingBean(value = [Redis::class])
class HostedRedis(@Value("\${redis.url}") val redisUrl: String) : Redis {

    override fun getClient(): RedisClient {
        return RedisClient.create(redisUrl)
    }
}