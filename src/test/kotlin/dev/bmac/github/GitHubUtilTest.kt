package dev.bmac.github

import dev.bmac.github.rest.Type
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.net.http.HttpHeaders

@UseExperimental(UnstableDefault::class)
class GitHubUtilTest {

    val clientId = "clientId"
    val clientSecret = "clientSecret"
    val code = "code"
    val state = "state"
    val token = "token"

    val mockWeb = MockWebServer()

    @Test
    fun testGhAuthenticate() {
        mockWeb.enqueue(MockResponse().setBody(Json.stringify(AccessToken.serializer(), AccessToken(token))))

        mockWeb.start()

        val host = "${mockWeb.hostName}:${mockWeb.port}"

        val gitHubUtil = GitHubUtil(host, host, clientId, clientSecret, "http://")
        val authToken = gitHubUtil.getAuthenticationToken(code, state)
        assertEquals(token, authToken)

        val request = mockWeb.takeRequest()
        assertEquals(request.requestUrl.queryParameter("client_id"), clientId)
        assertEquals(request.requestUrl.queryParameter("client_secret"), clientSecret)
        assertEquals(request.requestUrl.queryParameter("code"), code)
        assertEquals(request.requestUrl.queryParameter("state"), state)
        assertEquals(request.requestUrl.encodedPath(), "/login/oauth/access_token")
    }

    @Test
    fun testGhGpgUpload() {
        val error = GHErrors("",
            listOf(GHError("resource", "field", "code", "message")))
        val body = Json.stringify(GHErrors.serializer(), error)
        val statusCode = HttpStatus.I_AM_A_TEAPOT.value()
        val gpgKey = getPayload().gpgKey!!
        mockWeb.enqueue(MockResponse().setBody(body).setResponseCode(statusCode))

        val expectedStatus = Status(Type.GPG, statusCode, error)

        mockWeb.start()

        val host = "${mockWeb.hostName}:${mockWeb.port}"

        val gitHubUtil = GitHubUtil(host, host, clientId, clientSecret, "http://")
        val returnStatus = gitHubUtil.uploadGpgKey(gpgKey, token)
        assertEquals(expectedStatus, returnStatus)

        val request = mockWeb.takeRequest()
        assertEquals("token $token",
            request.headers.get(org.springframework.http.HttpHeaders.AUTHORIZATION))
        assertEquals(request.requestUrl.encodedPath(), "/user/gpg_keys")
    }

    @Test
    fun testGhSshUpload() {
        val error = GHErrors("",
            listOf(GHError("resource", "field", "code", "message")))
        val body = Json.stringify(GHErrors.serializer(), error)
        val statusCode = HttpStatus.I_AM_A_TEAPOT.value()
        val sshKey = getPayload().sshKey!!
        mockWeb.enqueue(MockResponse().setBody(body).setResponseCode(statusCode))

        val expectedStatus = Status(Type.SSH, statusCode, error)

        mockWeb.start()

        val host = "${mockWeb.hostName}:${mockWeb.port}"

        val gitHubUtil = GitHubUtil(host, host, clientId, clientSecret, "http://")
        val returnStatus = gitHubUtil.uploadSsshKey(sshKey, token)
        assertEquals(expectedStatus, returnStatus)

        val request = mockWeb.takeRequest()
        assertEquals("token $token",
            request.headers.get(org.springframework.http.HttpHeaders.AUTHORIZATION))
        assertEquals(request.requestUrl.encodedPath(), "/user/keys")
    }

}