package dev.bmac.github.web

import com.google.common.primitives.Longs
import dev.bmac.github.*
import dev.bmac.github.rest.State
import dev.bmac.github.rest.Type
import dev.bmac.github.storage.KeyStorage
import kotlinx.serialization.internal.HexConverter
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.util.UriComponentsBuilder
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.*
import java.util.logging.Logger
import javax.servlet.http.HttpServletResponse

@Controller
class WebController(val keyStorage: KeyStorage,
                    @Value("\${github.host}") val githubUrl: String,
                    @Value("\${github.api.host}") val githubApiUrl: String,
                    @Value("\${client.id}") val clientId: String,
                    @Value("\${client.secret}") val clientSecret: String) {

    private val logger = Logger.getLogger(this::class.simpleName)
    private val client = OkHttpClient()
    private val JSON = MediaType.get("application/json; charset=utf-8")

    @GetMapping("/initiate")
    fun initiate(response: HttpServletResponse, model: Model, @RequestParam("code") code: String,
                 @RequestParam("state") state: String): String? {
        val expire = keyStorage.getExpiration(state)
        if (expire <= 0) {
            response.status = HttpStatus.NOT_FOUND.value()
            return null
        }
        keyStorage.getPayload(state)?.let {
            it.sshKey?.let { key ->
                model.addAttribute("sshkey", getSshKeySignature(key))
            }
            it.gpgKey?.let { key ->
                model.addAttribute("gpgkey", getGpgFingerprint(key))
            }
        }
        model.addAttribute("code", code)
        model.addAttribute("state", state)
        model.addAttribute("expire", expire)
        return "verification"
    }

    @PostMapping("/perform")
    fun perform(response: HttpServletResponse, model: Model,
                @RequestParam("code") code: String, @RequestParam("state") state: String): String? {
        val payload = keyStorage.getPayload(state)
        if (payload == null) {
            response.status = HttpStatus.NOT_FOUND.value()
            return null
        }
        keyStorage.setState(state, State.IN_PROGRESS)
        val accessToken = authenticate(code, state)
        val status = upload(accessToken, payload)
        val success = status.all { it.isSuccess() }
        keyStorage.setState(state, if (success) State.COMPLETE else State.FAILED )
        model.addAttribute("status", status)
        model.addAttribute("success", success)
        return "result"
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

    private fun upload(accessToken: String, payload: Payload): List<Status> {
        val statusList = mutableListOf<Status>()
        if (payload.sshKey != null) {
            val json = Json.stringify(SshKey.serializer(), payload.sshKey)
            val body = okhttp3.RequestBody.create(JSON, json)
            val request = Request.Builder().url("https://${githubApiUrl}/user/keys")
                .header(HttpHeaders.AUTHORIZATION, "token $accessToken").post(body).build()
            client.newCall(request).execute().use {
                it.body().use { body ->
                    statusList.add(Status(Type.SSH, it.code(), body?.string()))
                }
            }
        }
        if (payload.gpgKey != null) {
            val body = okhttp3.RequestBody.create(JSON, Json.stringify(GpgKey.serializer(), payload.gpgKey))
            val request = Request.Builder().url("https://${githubApiUrl}/user/gpg_keys")
                .header(HttpHeaders.AUTHORIZATION, "token $accessToken").post(body).build()
            client.newCall(request).execute().use {
                it.body().use { body ->
                    statusList.add(Status(Type.GPG, it.code(), body?.string()))
                }
            }
        }
        return statusList
    }
}

fun getSshKeySignature(ssh: SshKey): String {
    val key = ssh.key
    val middle = key.split(" ")[1]
    val decoded = Base64.getDecoder().decode(middle)
    val md = MessageDigest.getInstance("MD5")
    md.update(decoded)
    val hex = HexConverter.printHexBinary(md.digest(), true)
    var output = ""
    for (i in 0 until hex.length step 2) {
        output += hex[i].toString() + hex[i+1].toString() + ":"
    }
    return output.substringBeforeLast(':')
}

fun getGpgFingerprint(gpgKey: GpgKey): String {
    val key = getGpgPublicKey(gpgKey.armored_public_key)
    return HexConverter.printHexBinary(Longs.toByteArray(key.keyID))
}

private fun getGpgPublicKey(key: String): PGPPublicKey {
    val ringCollection = PGPPublicKeyRingCollection(ArmoredInputStream(ByteArrayInputStream(key.toByteArray())),
        JcaKeyFingerprintCalculator())
    ringCollection.keyRings.forEach {
        it.publicKeys.forEach {
            if (it.isEncryptionKey) {
                return it
            }
        }
    }
    throw IllegalStateException("Cant find key")
}