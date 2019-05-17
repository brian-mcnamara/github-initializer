package dev.bmac.github.storage

import dev.bmac.github.rest.Payload
import io.lettuce.core.RedisClient
import io.lettuce.core.api.sync.RedisCommands
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.lang.RuntimeException

@Service
@UseExperimental(kotlinx.serialization.UnstableDefault::class)
class KeyStorage(@Value("\${redis.url}") redisHost: String) {
    private final val redisClient = RedisClient.create(redisHost)

    fun addPayload(key: String, payload: Payload) {
        val conn = getConnection()
        conn.set(key, Json.stringify(Payload.serializer(), payload))
        conn.expire(key, 60 * 2)
    }

    fun getPayload(key: String): Payload? {
        val conn = getConnection()
        val json = conn.get(key) ?: return null
        return Json.parse(Payload.serializer(), json)
    }

    fun getExpiration(key: String): Long {
        val conn = getConnection()
        return conn.ttl(key)
    }

    private fun getConnection() : RedisCommands<String, String> {
        val conn = redisClient.connect()
        return conn.sync()
    }
}