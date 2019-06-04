package dev.bmac.github.rest

import dev.bmac.github.Payload
import dev.bmac.github.UploadResponse
import dev.bmac.github.getPayload
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
}

@UseExperimental(UnstableDefault::class)
fun uploadKeys(restTemplate: TestRestTemplate, payload: Payload): ResponseEntity<UploadResponse> {
    val headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_JSON

    val httpEntity = HttpEntity(Json.stringify(Payload.serializer(), payload), headers)
    return restTemplate.postForEntity("/upload", httpEntity, UploadResponse::class.java)
}