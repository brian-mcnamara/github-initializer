package dev.bmac.github.web

import dev.bmac.github.GitHubUtil
import dev.bmac.github.Status
import dev.bmac.github.getPayload
import dev.bmac.github.rest.Type
import dev.bmac.github.storage.KeyStorage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@ExtendWith(SpringExtension::class)
@WebMvcTest(WebController::class)
class WebControllerITest(@Autowired val mockMvc: MockMvc) {

    @MockBean
    lateinit var keyStorage: KeyStorage
    @MockBean
    lateinit var gitHubUtil: GitHubUtil

    @Test
    fun testConfirmPage() {
        val kid = "test"
        val payload = getPayload()
        Mockito.`when`(keyStorage.getPayload(kid)).thenReturn(payload)
        Mockito.`when`(keyStorage.getExpiration(kid)).thenReturn(100)
        val code = "1234"
        mockMvc.perform(MockMvcRequestBuilders.get("/initiate?code=$code&state=$kid"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.xpath("//form/input[@name='code']/@value").string(code))
            .andExpect(MockMvcResultMatchers.xpath("//form/input[@name='state']/@value").string(kid))
            .andExpect(MockMvcResultMatchers.xpath("//form/input[@name='csrf']/@value").exists())
    }

    @Test
    fun testPerformPage() {
        val code = "1234"
        val auth = "authToken"
        val kid = "test"
        val csrf = "csrf"
        val payload = getPayload()
        Mockito.`when`(keyStorage.getPayload(kid)).thenReturn(payload)
        Mockito.`when`(keyStorage.getExpiration(kid)).thenReturn(100)
        Mockito.`when`(keyStorage.getCSRF(kid)).thenReturn(csrf)
        Mockito.`when`(gitHubUtil.getAuthenticationToken(code, kid)).thenReturn(auth)
        Mockito.`when`(gitHubUtil.uploadGpgKey(payload.gpgKey!!, auth)).thenReturn(Status(Type.GPG, 201, ""))
        Mockito.`when`(gitHubUtil.uploadSsshKey(payload.sshKey!!, auth)).thenReturn(Status(Type.SSH, 201, ""))
        mockMvc.perform(MockMvcRequestBuilders.post("/perform?code=$code&state=$kid&csrf=$csrf"))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }
}