package dev.bmac.github.storage

import dev.bmac.github.Payload
import dev.bmac.github.rest.State
import io.lettuce.core.RedisClient
import io.lettuce.core.api.sync.RedisCommands
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@UseExperimental(kotlinx.serialization.UnstableDefault::class)
class KeyStorage(@Value("\${redis.url}") redisHost: String) {
    private final val redisClient = RedisClient.create(redisHost)
    val conn = getConnection()
    private val stateKey = { key: String -> "state:$key" }

    fun addPayload(key: String, payload: Payload) {
        conn.set(key, Json.stringify(Payload.serializer(), payload))
        conn.expire(key, 60 * 2)
        conn.set(stateKey(key), State.INITIATED.name)
        conn.expire(stateKey(key), 60 * 60)
    }

    fun getPayload(key: String): Payload? {
        val json = conn.get(key) ?: return null
        return Json.parse(Payload.serializer(), json)
    }

    fun getExpiration(key: String): Long {
        return conn.ttl(key)
    }

    fun setState(key: String, state: State) {
        conn.set(stateKey(key), state.name)
    }

    fun getState(key: String): State {
        val state = conn.get(stateKey(key)) ?: return State.UNKNOWN
        return State.valueOf(state)
    }

    private fun getConnection() : RedisCommands<String, String> {
        val conn = redisClient.connect()
        return conn.sync()
    }
}