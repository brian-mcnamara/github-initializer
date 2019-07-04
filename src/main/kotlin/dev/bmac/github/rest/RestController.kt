package dev.bmac.github.rest

import dev.bmac.github.Payload
import dev.bmac.github.TransactionState
import dev.bmac.github.UploadResponse
import dev.bmac.github.storage.KeyStorage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RestController
import java.util.*
import javax.servlet.http.HttpServletResponse

@UseExperimental(kotlinx.serialization.UnstableDefault::class)
@RestController
class RestController(val keyStorage: KeyStorage,
                     @Value("\${github.host}") val githubUrl: String,
                     @Value("\${client.id}") val clientId: String) {

    private val json = Json(JsonConfiguration(encodeDefaults = false))

    @PostMapping("/upload", produces = ["application/json"])
    fun upload(@RequestBody payload: Payload): String {
        val uuid = UUID.randomUUID().toString().replace("-", "")
        keyStorage.addPayload(uuid, payload)
        val redirect = "https://${githubUrl}/login/oauth/authorize?client_id=${clientId}&state=${uuid}&scope=write:public_key,write:gpg_key"
        val response = UploadResponse(uuid, redirect)
        return json.stringify(UploadResponse.serializer(), response)
    }

    @GetMapping("/status")
    fun status(@RequestParam("id") uuid: String): ResponseEntity<String> {
        val state = keyStorage.getState(uuid)
        if (!state.isValid()) {
            return ResponseEntity(HttpStatus.NOT_FOUND)
        } else if (!state.isComplete()) {
            return ResponseEntity(json.stringify(TransactionState.serializer(), state), HttpStatus.PROCESSING)
        }
        return ResponseEntity.ok(json.stringify(TransactionState.serializer(), state))
    }
}