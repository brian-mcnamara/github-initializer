package dev.bmac.github.storage

import dev.bmac.github.Payload
import dev.bmac.github.TransactionState
import io.lettuce.core.api.sync.RedisCommands
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@UseExperimental(kotlinx.serialization.UnstableDefault::class)
class KeyStorage(@Autowired redis: Redis) {
    private final val redisClient = redis.getClient()
    val conn = getConnection()
    private val stateKey = { key: String -> "state:$key" }
    private val csrfKey = { key: String -> "csrf:$key" }

    fun addPayload(key: String, payload: Payload) {
        conn.set(key, Json.stringify(Payload.serializer(), payload))
        conn.expire(key, 60 * 2)
        val state = TransactionState()
        if (payload.gpgKey != null) state.initGpgKey()
        if (payload.sshKey != null) state.initSshKey()
        setState(key, state)
        conn.expire(stateKey(key), 60 * 60)
    }

    fun getPayload(key: String): Payload? {
        val json = conn.get(key) ?: return null
        return Json.parse(Payload.serializer(), json)
    }

    fun getExpiration(key: String): Long {
        return conn.ttl(key)
    }

    fun setState(key: String, state: TransactionState) {
        conn.set(stateKey(key), Json.stringify(TransactionState.serializer(), state))
    }

    fun getState(key: String): TransactionState {
        val state: String = conn.get(stateKey(key)) ?: return TransactionState()
        return Json.parse(TransactionState.serializer(), state)
    }

    fun setCSRF(key: String, csrf: String) {
        conn.set(csrfKey(key), csrf);
        conn.expire(csrfKey(key), 60 * 2)
    }

    fun getCSRF(key: String): String? {
        return conn.get(csrfKey(key))
    }

    private fun getConnection() : RedisCommands<String, String> {
        val conn = redisClient.connect()
        return conn.sync()
    }
}