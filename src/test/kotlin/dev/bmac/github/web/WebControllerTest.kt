package dev.bmac.github.web

import dev.bmac.github.rest.GpgKey
import dev.bmac.github.rest.SshKey
import org.junit.Assert
import org.junit.Test

class WebControllerTest {
    @Test
    fun testSshFingerprint() {
        "sshkey.pub".asResource {
            val signature = getSshKeySignature(SshKey(it, ""))
            Assert.assertEquals("e2:f4:a4:25:c3:b2:ad:9e:dc:88:c6:35:7b:02:00:82", signature)
        }
    }

    @Test
    fun testGpgFingerprint() {
        "gpgkey.pub".asResource {
            val signature = getGpgFingerprint(GpgKey(it))
            Assert.assertEquals("A81C74651B83E45C", signature)
        }
    }
}

fun String.asResource(work: (String) -> Unit) {
    val content = ClassLoader.getSystemClassLoader().getResource(this).readText()
    work(content)
}