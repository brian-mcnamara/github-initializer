package dev.bmac.github.web

import com.google.common.primitives.Longs
import dev.bmac.github.*
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
                 @RequestParam("state") id: String): String? {
        val expire = keyStorage.getExpiration(id)
        if (expire <= 0) {
            response.status = HttpStatus.NOT_FOUND.value()
            return null
        }
        keyStorage.getPayload(id)?.let {
            it.sshKey?.let { key ->
                model.addAttribute("sshkey", getSshKeySignature(key))
            }
            it.gpgKey?.let { key ->
                model.addAttribute("gpgkey", getGpgFingerprint(key))
            }
        }

        //Probably not needed, but here to make sure users navigate through the confirmation page.
        val csrf = UUID.randomUUID().toString().replace("-", "")
        keyStorage.setCSRF(id, csrf)
        model.addAttribute("csrf", csrf)
        model.addAttribute("code", code)
        model.addAttribute("id", id)
        model.addAttribute("expire", expire)
        return "verification"
    }

    @PostMapping("/perform")
    fun perform(response: HttpServletResponse, model: Model,
                @RequestParam("code") code: String, @RequestParam("id") id: String,
                @RequestParam("csrf") csrf: String): String? {
        val payload = keyStorage.getPayload(id)
        if (payload == null) {
            response.status = HttpStatus.NOT_FOUND.value()
            return null
        }
        val storedCsrf = keyStorage.getCSRF(id)
        if (storedCsrf == null || !storedCsrf.equals(csrf)) {
            response.status = HttpStatus.FORBIDDEN.value()
            return null
        }
        val accessToken = gitHubUtil.getAuthenticationToken(code, id)
        val status = upload(accessToken, payload)
        val state = keyStorage.getState(id)
        status.status.forEach {
            when (it.type) {
                KeyType.SSH -> state.sshComplete(it.errors?.errors?.get(0)?.message)
                KeyType.GPG -> state.gpgComplete(it.errors?.errors?.get(0)?.message)
            }
        }
        keyStorage.setState(id, state)
        model.addAttribute("status", status)
        return "result"
    }

    private fun upload(accessToken: String, payload: Payload): StatusList {
        val statusList = mutableListOf<Status>()
        if (payload.sshKey != null) {
            statusList.add(gitHubUtil.uploadSsshKey(payload.sshKey, accessToken))
        }
        if (payload.gpgKey != null) {
            statusList.add(gitHubUtil.uploadGpgKey(payload.gpgKey, accessToken))
        }
        return StatusList(statusList)
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