package dev.bmac.github

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class UploadResponse(val id: String, val redirect: String)

@Serializable
data class Payload(val sshKey: SshKey?, val gpgKey: GpgKey?)

@Serializable
data class AccessToken(val access_token: String)

enum class Progress {
    IN_PROGRESS, COMPLETE, UNKNOWN
}

enum class KeyType{ GPG, SSH }

@Serializable
data class State(val type: KeyType, val progress: Progress, val error: String? = null)

@Serializable
data class TransactionState(var gpgStatus: State? = null, var sshStatus: State? = null) {

    fun initSshKey() {
        sshStatus = State(KeyType.SSH, Progress.IN_PROGRESS)
    }

    fun initGpgKey() {
        gpgStatus = State(KeyType.GPG, Progress.IN_PROGRESS)
    }

    fun sshComplete(error: String? = null) {
        sshStatus = State(KeyType.SSH, Progress.COMPLETE, error)
    }

    fun gpgComplete(error: String? = null) {
        gpgStatus = State(KeyType.GPG, Progress.COMPLETE, error)
    }

    fun isValid(): Boolean {
        return gpgStatus != null || sshStatus != null
    }

    fun isComplete(): Boolean {
        return isValid() && (gpgStatus == null || gpgStatus!!.progress.equals(Progress.COMPLETE))
                && (sshStatus == null || sshStatus!!.progress.equals(Progress.COMPLETE))
    }
}