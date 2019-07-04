package dev.bmac.github.web

import dev.bmac.github.*
import dev.bmac.github.storage.KeyStorage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
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

    private val code = "1234"
    private val auth = "authToken"
    private val kid = "test"
    private val csrf = "csrf"
    private val payload = getPayload()

    @Test
    fun testConfirmPage() {
        Mockito.`when`(keyStorage.getPayload(kid)).thenReturn(payload)
        Mockito.`when`(keyStorage.getExpiration(kid)).thenReturn(100)
        mockMvc.perform(MockMvcRequestBuilders.get("/initiate?code=$code&state=$kid"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.xpath("//form/input[@name='code']/@value").string(code))
            .andExpect(MockMvcResultMatchers.xpath("//form/input[@name='id']/@value").string(kid))
            .andExpect(MockMvcResultMatchers.xpath("//form/input[@name='csrf']/@value").exists())
    }

    @Test
    fun testPerformPage() {
        setupKeystore()
        setupAccessToken()
        Mockito.`when`(gitHubUtil.uploadGpgKey(payload.gpgKey!!, auth)).thenReturn(Status(KeyType.GPG, 201))
        Mockito.`when`(gitHubUtil.uploadSsshKey(payload.sshKey!!, auth)).thenReturn(Status(KeyType.SSH, 201))
        mockMvc.perform(MockMvcRequestBuilders.post("/perform?code=$code&id=$kid&csrf=$csrf"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.xpath("//div[@id='success']").exists())
    }

    @Test
    fun testPerformPageWithBadResult() {
        setupKeystore()
        setupAccessToken()
        val message = "Error with GPG"
        Mockito.`when`(gitHubUtil.uploadGpgKey(payload.gpgKey!!, auth)).thenReturn(Status(KeyType.GPG, 500, GHErrors("error",
            listOf(GHError("GPG", "GPG", "unknown", message)))))
        Mockito.`when`(gitHubUtil.uploadSsshKey(payload.sshKey!!, auth)).thenReturn(Status(KeyType.SSH, 201))
        mockMvc.perform(MockMvcRequestBuilders.post("/perform?code=$code&id=$kid&csrf=$csrf"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.xpath("//div[@id='errors']").exists())
            .andExpect(MockMvcResultMatchers.xpath("//li[@id='GPG']").exists())
            .andExpect(MockMvcResultMatchers.xpath("//li[@id='GPG']/p").string(message))
            .andExpect(MockMvcResultMatchers.xpath("//li[@id='SSH']").doesNotExist())
    }

    @Test
    fun testPerformPageWithTwoBadResult() {
        setupKeystore()
        setupAccessToken()
        val gpgMessage = "Error with GPG"
        val sshMessage = "Error with ssh"
        Mockito.`when`(gitHubUtil.uploadGpgKey(payload.gpgKey!!, auth)).thenReturn(Status(KeyType.GPG, 500,
            GHErrors("", listOf(GHError("", "", "", gpgMessage)))
        ))
        Mockito.`when`(gitHubUtil.uploadSsshKey(payload.sshKey!!, auth)).thenReturn(Status(KeyType.SSH, 500,
            GHErrors("", listOf(GHError("", "", "", sshMessage)))
        ))
        mockMvc.perform(MockMvcRequestBuilders.post("/perform?code=$code&id=$kid&csrf=$csrf"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.xpath("//div[@id='errors']").exists())
            .andExpect(MockMvcResultMatchers.xpath("//li[@id='GPG']").exists())
            .andExpect(MockMvcResultMatchers.xpath("//li[@id='SSH']").exists())
            .andExpect(MockMvcResultMatchers.xpath("//li[@id='SSH']/p").string(sshMessage))
            .andExpect(MockMvcResultMatchers.xpath("//li[@id='GPG']/p").string(gpgMessage))

    }

    @Test
    fun testPerformBadCsrf() {
        setupKeystore()
        setupAccessToken()
        mockMvc.perform(MockMvcRequestBuilders.post("/perform?code=$code&id=$kid&csrf=invalid"))
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.FORBIDDEN.value()))

    }

    @Test
    fun testPerformBadid() {
        setupKeystore()
        setupAccessToken()
        mockMvc.perform(MockMvcRequestBuilders.post("/perform?code=$code&id=badId&csrf=$csrf"))
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.NOT_FOUND.value()))

    }

    private fun setupKeystore() {
        Mockito.`when`(keyStorage.getPayload(kid)).thenReturn(payload)
        Mockito.`when`(keyStorage.getExpiration(kid)).thenReturn(100)
        Mockito.`when`(keyStorage.getCSRF(kid)).thenReturn(csrf)
        Mockito.`when`(keyStorage.getState(kid)).thenReturn(TransactionState())
    }

    private fun setupAccessToken() {
        Mockito.`when`(gitHubUtil.getAuthenticationToken(code, kid)).thenReturn(auth)
    }
}