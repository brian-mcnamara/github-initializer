package dev.bmac.github.rest

import dev.bmac.github.storage.KeyStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder
import java.lang.Exception
import java.lang.RuntimeException
import java.util.*
import java.util.logging.Logger

@RestController
@UseExperimental(kotlinx.serialization.UnstableDefault::class)
class RestController(keyStorage: KeyStorage, @Value("\${github.host}") githubUrl: String,
                     @Value("\${github.api.host}") githubApiUrl: String,
                     @Value("\${client.id}") clientId: String,
                     @Value("\${client.secret}") clientSecret: String) {
    final val logger = Logger.getLogger(this::class.simpleName)
    val keyStorage = keyStorage
    val githubUrl = githubUrl
    val githubApiUrl = githubApiUrl
    val clientId = clientId
    val clientSecret = clientSecret
    val client = OkHttpClient()
    val JSON = MediaType.get("application/json; charset=utf-8")

    @PostMapping("/upload")
    fun post(@RequestBody payload: Payload): String {
        val uuid = UUID.randomUUID().toString()
        keyStorage.addPayload(uuid, payload)
        return "https://${githubUrl}/login/oauth/authorize?client_id=${clientId}&state=${uuid}&scope=write:public_key,write:gpg_key"
    }

    @GetMapping("/perform")
    fun perform(@RequestParam("code") code: String, @RequestParam("state") state: String): ResponseEntity<String> {
        val payload = keyStorage.getPayload(state) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val accessToken = authenticate(code, state)
        val status = upload(accessToken, payload)
        return ResponseEntity(status)
    }

    private fun authenticate(code: String, state: String): String {
        val url = UriComponentsBuilder.fromPath("https://${githubUrl}/login/oauth/access_token")
            .queryParam("client_id", clientId)
            .queryParam("client_secret", clientSecret)
            .queryParam("code", code)
            .queryParam("state", state).build().toUriString()
        val request = Request.Builder().url(url).header(HttpHeaders.ACCEPT, "application/json").build()
        client.newCall(request).execute().use {
            val accessToken = Json.nonstrict.parse(AccessToken.serializer(), it.body()!!.string())
            return accessToken.access_token
        }
    }

    private fun upload(accessToken: String, payload: Payload): HttpStatus {
        var sshStatus = 201
        var gpgStatus = 201
        if (payload.sshKey != null) {
            val json = Json.stringify(SshKey.serializer(), payload.sshKey)
            val body = okhttp3.RequestBody.create(JSON, json)
            val request = Request.Builder().url("https://${githubApiUrl}/user/keys")
                .header(HttpHeaders.AUTHORIZATION, "token $accessToken").post(body).build()
            client.newCall(request).execute().use {
                if (it.code() != HttpStatus.CREATED.value()) {
                    sshStatus = it.code()
                    it.body().use {body ->
                        logger.warning("Failed to upload user ssh key: $sshStatus\n ${body?.string()} ")
                    }
                }
            }
        }
        if (payload.gpgKey != null) {
            val body = okhttp3.RequestBody.create(JSON, Json.stringify(GpgKey.serializer(), payload.gpgKey))
            val request = Request.Builder().url("https://${githubApiUrl}/user/gpg_keys")
                .header(HttpHeaders.AUTHORIZATION, "token $accessToken").post(body).build()
            client.newCall(request).execute().use {
                if (it.code() != HttpStatus.CREATED.value()) {
                    gpgStatus = it.code()
                    it.body().use { body ->
                        logger.warning("Failed to upload user gpg key: $gpgStatus\n ${body?.string()} ")
                    }
                }
            }
        }
        return HttpStatus.CREATED
    }
}

@Serializable
data class SshKey(val key: String, val title: String)
@Serializable
data class GpgKey(val armored_public_key: String)
@Serializable
data class Payload(val sshKey: SshKey?, val gpgKey: GpgKey?)
@Serializable
data class AccessToken(val access_token: String)