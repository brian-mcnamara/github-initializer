package dev.bmac.github.web

import dev.bmac.github.GpgKey
import dev.bmac.github.SshKey
import dev.bmac.github.getArmoredPublicGpgKey
import dev.bmac.github.getPublicSshKey
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class WebControllerTest {
    @Test
    fun testSshFingerprint() {
        val key = getPublicSshKey()
        val signature = getSshKeySignature(SshKey(key, ""))
        Assertions.assertEquals("e2:f4:a4:25:c3:b2:ad:9e:dc:88:c6:35:7b:02:00:82", signature)
    }

    @Test
    fun testGpgFingerprint() {
        val key = getArmoredPublicGpgKey()
        val signature = getGpgFingerprint(GpgKey(key))
        Assertions.assertEquals("A81C74651B83E45C", signature)
    }
}

