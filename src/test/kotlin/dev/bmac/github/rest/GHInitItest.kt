package dev.bmac.github.rest

import dev.bmac.github.*
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GHInitItest(@Value("\${github.host}") val host: String, @Autowired val restTemplate: TestRestTemplate) {

    @Test
    fun testKeyUpload() {
        val payload = getPayload()
        val response = uploadKeys(restTemplate, payload)

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        Assertions.assertNotNull(response.body)
        Assertions.assertTrue(response.body!!.id.isNotBlank())
        Assertions.assertEquals("https://$host/login/oauth/authorize?client_id=&state=${response.body!!.id}&scope=write:public_key,write:gpg_key",
            response.body!!.redirect)

    }

    @Test
    fun testStatusWithoutUpload() {
        val response = restTemplate.getForEntity("/status?id=test", String::class.java)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun testStatus() {
        val payload = getPayload()
        val uploadResponse = uploadKeys(restTemplate, payload)

        val response = restTemplate.getForEntity("/status?id=${uploadResponse.body!!.id}", TransactionState::class.java)

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        Assertions.assertNotNull(response.body)

        Assertions.assertEquals(Progress.IN_PROGRESS, response.body!!.gpgStatus!!.progress)
        Assertions.assertEquals(Progress.IN_PROGRESS, response.body!!.sshStatus!!.progress)
        Assertions.assertNull(response.body!!.gpgStatus!!.error)
        Assertions.assertNull(response.body!!.sshStatus!!.error)
    }
}

@UseExperimental(UnstableDefault::class)
fun uploadKeys(restTemplate: TestRestTemplate, payload: Payload): ResponseEntity<UploadResponse> {
    val headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_JSON

    val httpEntity = HttpEntity(Json.stringify(Payload.serializer(), payload), headers)
    return restTemplate.postForEntity("/upload", httpEntity, UploadResponse::class.java)
}