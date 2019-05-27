package dev.bmac.github.rest

import dev.bmac.github.Payload
import dev.bmac.github.UploadResponse
import dev.bmac.github.storage.KeyStorage
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class RestController(val keyStorage: KeyStorage,
                     @Value("\${github.host}") val githubUrl: String,
                     @Value("\${client.id}") val clientId: String) {

    @UseExperimental(kotlinx.serialization.UnstableDefault::class)
    @PostMapping("/upload", produces = ["application/json"])
    fun upload(@RequestBody payload: Payload): String {
        val uuid = UUID.randomUUID().toString().replace("-", "")
        keyStorage.addPayload(uuid, payload)
        val redirect = "https://${githubUrl}/login/oauth/authorize?client_id=${clientId}&state=${uuid}&scope=write:public_key,write:gpg_key"
        val response = UploadResponse(uuid, redirect)
        return Json.stringify(UploadResponse.serializer(), response)
    }

    @GetMapping("/status")
    fun status(@RequestParam("uuid") uuid: String): String {
        return keyStorage.getState(uuid).name
    }
}

enum class State {
    INITIATED, IN_PROGRESS, COMPLETE, FAILED, UNKNOWN
}

enum class Type {
    GPG, SSH
}