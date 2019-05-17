package dev.bmac.github.web

import com.google.common.primitives.Longs
import dev.bmac.github.rest.GpgKey
import dev.bmac.github.rest.SshKey
import dev.bmac.github.storage.KeyStorage
import kotlinx.serialization.internal.HexConverter
import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.*
import javax.servlet.http.HttpServletResponse

@Controller
class WebController(keyStorage: KeyStorage, @Value("\${client.id}") clientId: String,
                    @Value("\${client.secret}") clientSecret: String) {
    val keyStorage = keyStorage
    val clientId = clientId
    val clientSecret = clientSecret

    @GetMapping("/initiate")
    fun initiate(response: HttpServletResponse, model: Model, @RequestParam("code") code: String,
                 @RequestParam("state") state: String): String? {
        val expire = keyStorage.getExpiration(state)
        if (expire <= 0) {
            response.status = 404
            return null
        }
        val payload = keyStorage.getPayload(state)
        //TODO is there a kotlin way to do this?
        if (payload.sshKey != null) {
            model.addAttribute("sshkey", getSshKeySignature(payload.sshKey))
        }
        if (payload.gpgKey != null) {
            model.addAttribute("gpgkey", getGpgFingerprint(payload.gpgKey))
        }
        model.addAttribute("code", code)
        model.addAttribute("state", state)
        model.addAttribute("expire", expire)
        return "verification"
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