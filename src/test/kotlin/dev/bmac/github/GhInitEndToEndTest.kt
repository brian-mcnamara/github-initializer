package dev.bmac.github

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput
import dev.bmac.github.rest.Type
import dev.bmac.github.rest.uploadKeys
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GhInitEndToEndTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate
    @MockBean
    lateinit var gitHubUtil: GitHubUtil

    private val accessToken = "accessToken"

    @Test
    fun testEndToEnd() {
        val payload = getPayload()
        val response = uploadKeys(restTemplate, payload)
        assertTrue(response.statusCode.is2xxSuccessful)

        val code = "1234"
        val uuid = response.body!!.id

        Mockito.`when`(gitHubUtil.getAuthenticationToken(code, uuid)).thenReturn(accessToken)
        Mockito.`when`(gitHubUtil.uploadSsshKey(payload.sshKey!!, accessToken)).thenReturn(Status(Type.SSH, 201))
        Mockito.`when`(gitHubUtil.uploadGpgKey(payload.gpgKey!!, accessToken)).thenReturn(Status(Type.GPG, 201))
        WebClient().use {
            var page = it.getPage<HtmlPage>("${restTemplate.rootUri}/initiate?code=$code&state=$uuid")
            val form = page.getFormByName("verification")
            assertNotNull(form)
            val confirm = form.getInputByName<HtmlSubmitInput>("accept")
            assertNotNull(confirm)
            page = confirm.click()
            assertNotNull(page.getElementById("success"))
            Mockito.verify(gitHubUtil).uploadGpgKey(payload.gpgKey!!, accessToken)
            Mockito.verify(gitHubUtil).uploadSsshKey(payload.sshKey!!, accessToken)
        }
    }
}