package dev.bmac.github.web

import com.google.common.primitives.Longs
import dev.bmac.github.*
import dev.bmac.github.rest.State
import dev.bmac.github.storage.KeyStorage
import kotlinx.serialization.internal.HexConverter
import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.*
import java.util.logging.Logger
import javax.servlet.http.HttpServletResponse

@Controller
class WebController(val keyStorage: KeyStorage, val gitHubUtil: GitHubUtil) {

    private val logger = Logger.getLogger(this::class.simpleName)
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
        val accessToken = gitHubUtil.getAuthenticationToken(code, state)
        val status = upload(accessToken, payload)
        val success = status.all { it.isSuccess() }
        keyStorage.setState(state, if (success) State.COMPLETE else State.FAILED )
        model.addAttribute("status", status)
        model.addAttribute("success", success)
        return "result"
    }

    private fun upload(accessToken: String, payload: Payload): List<Status> {
        val statusList = mutableListOf<Status>()
        if (payload.sshKey != null) {
            statusList.add(gitHubUtil.uploadSsshKey(payload.sshKey, accessToken))
        }
        if (payload.gpgKey != null) {
            statusList.add(gitHubUtil.uploadGpgKey(payload.gpgKey, accessToken))
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