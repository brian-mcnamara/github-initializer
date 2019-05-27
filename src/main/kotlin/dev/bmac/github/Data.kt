package dev.bmac.github

import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(val uuid: String, val redirect: String)

@Serializable
data class Payload(val sshKey: SshKey?, val gpgKey: GpgKey?)

@Serializable
data class AccessToken(val access_token: String)

@Serializable
data class GHError(val resource: String, val field: String, val code: String, val message: String)
