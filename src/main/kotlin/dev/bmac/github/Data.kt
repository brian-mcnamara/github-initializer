package dev.bmac.github

import dev.bmac.github.rest.Type
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.springframework.http.HttpStatus

@Serializable
data class UploadResponse(val uuid: String, val redirect: String)

@Serializable
data class SshKey(val key: String, val title: String)

@Serializable
data class GpgKey(val armored_public_key: String)

@Serializable
data class Payload(val sshKey: SshKey?, val gpgKey: GpgKey?)

@Serializable
data class AccessToken(val access_token: String)

@Serializable
data class GHErrors(val message: String, val errors: List<GHError>)

@Serializable
data class GHError(val resource: String, val field: String, val code: String, val message: String)

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