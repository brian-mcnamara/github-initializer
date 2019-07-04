package dev.bmac.github

import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.util.stream.Collectors

@Serializable
data class SshKey(val key: String, val title: String)

@Serializable
data class GpgKey(val armored_public_key: String)

@Serializable
data class GHError(val resource: String, val field: String, val code: String, val message: String)

@Serializable
data class GHErrors(val message: String, val errors: List<GHError>)

@UseExperimental(kotlinx.serialization.UnstableDefault::class)
data class Status(val type: KeyType, val status: Int, val errors: GHErrors? = null) {
    constructor(type: KeyType, status: Int, body: String?): this(type, status, if (body == null) null else Json.nonstrict.parse(GHErrors.serializer(), body))
    fun isSuccess(): Boolean {
        return status == HttpStatus.CREATED.value()
    }
}

data class StatusList(val status: List<Status>) {
    fun isSuccessful(): Boolean {
        return status.all { it.isSuccess() }
    }

    fun getErrors(): List<Status> {
        return status.stream().filter { !it.isSuccess() }.collect(Collectors.toList())
    }
}

@Component
@UseExperimental(UnstableDefault::class)
class GitHubUtil(@Value("\${github.api.host}") val githubApiUrl: String,
                 @Value("\${github.host}") val githubUrl: String,
                 @Value("\${client.id}") val clientId: String,
                 @Value("\${client.secret}") val clientSecret: String,
                 val protocol: String = "https://") {
    private val JSON = MediaType.get("application/json; charset=utf-8")
    private val client = OkHttpClient()

    fun getAuthenticationToken(code: String, state: String): String {
        val url = UriComponentsBuilder.fromPath("$protocol$githubUrl/login/oauth/access_token")
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

    fun uploadSsshKey(sshKey: SshKey, accessToken: String): Status {
        val json = Json.stringify(SshKey.serializer(), sshKey)
        val requestBody = okhttp3.RequestBody.create(JSON, json)
        val request = Request.Builder().url("$protocol$githubApiUrl/user/keys")
            .header(HttpHeaders.AUTHORIZATION, "token $accessToken").post(requestBody).build()
        client.newCall(request).execute().use {
            it.body()?.use { body ->
                return Status(KeyType.SSH, it.code(), if (it.code() == 201) null else body.string())
            }
        }
        return Status(KeyType.SSH, 500)
    }

    fun uploadGpgKey(gpgKey: GpgKey, accessToken: String): Status {
        val requestBody = okhttp3.RequestBody.create(JSON, Json.stringify(GpgKey.serializer(), gpgKey))
        val request = Request.Builder().url("$protocol$githubApiUrl/user/gpg_keys")
            .header(HttpHeaders.AUTHORIZATION, "token $accessToken").post(requestBody).build()
        client.newCall(request).execute().use {
            it.body()?.use { body ->
                return Status(KeyType.GPG, it.code(), if (it.code() == 201) null else body.string())
            }
        }
        return Status(KeyType.GPG, 500)
    }

}