package dev.bmac.github

import dev.bmac.github.rest.Type
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

@Serializable
data class SshKey(val key: String, val title: String)

@Serializable
data class GpgKey(val armored_public_key: String)

@Serializable
data class GHErrors(val message: String, val errors: List<GHError>)

@UseExperimental(kotlinx.serialization.UnstableDefault::class)
class Status(val type: Type, val status: Int, val body: String?) {
    fun isSuccess(): Boolean {
        return status == HttpStatus.CREATED.value()
    }

    fun getError(): GHErrors? {
        if (status != HttpStatus.UNPROCESSABLE_ENTITY.value()) {
            return null
        }
        return Json.nonstrict.parse(GHErrors.serializer(), body ?: "")
    }
}

@Component
@UseExperimental(UnstableDefault::class)
class GitHubUtil(@Value("\${github.api.host}") val githubApiUrl: String,
                 @Value("\${github.host}") val githubUrl: String,
                 @Value("\${client.id}") val clientId: String,
                 @Value("\${client.secret}") val clientSecret: String) {
    private val JSON = MediaType.get("application/json; charset=utf-8")
    private val client = OkHttpClient()

    fun getAuthenticationToken(code: String, state: String): String {
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

    fun uploadSsshKey(sshKey: SshKey, accessToken: String): Status {
        val json = Json.stringify(SshKey.serializer(), sshKey)
        val body = okhttp3.RequestBody.create(JSON, json)
        val request = Request.Builder().url("https://${githubApiUrl}/user/keys")
            .header(HttpHeaders.AUTHORIZATION, "token $accessToken").post(body).build()
        client.newCall(request).execute().use {
            it.body().use { body ->
                return Status(Type.SSH, it.code(), body?.string())
            }
        }
    }

    fun uploadGpgKey(gpgKey: GpgKey, accessToken: String): Status {
        val body = okhttp3.RequestBody.create(JSON, Json.stringify(GpgKey.serializer(), gpgKey))
        val request = Request.Builder().url("https://${githubApiUrl}/user/gpg_keys")
            .header(HttpHeaders.AUTHORIZATION, "token $accessToken").post(body).build()
        client.newCall(request).execute().use {
            it.body().use { body ->
                return Status(Type.GPG, it.code(), body?.string())
            }
        }
    }

}