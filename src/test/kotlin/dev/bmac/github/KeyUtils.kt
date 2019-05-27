package dev.bmac.github

fun getPublicSshKey(): String {
    return ClassLoader.getSystemResource("sshkey.pub").readText()
}

fun getArmoredPublicGpgKey(): String {
    return ClassLoader.getSystemResource("gpgkey.pub").readText()
}

fun getPayload() : Payload {
    val sshkey = getPublicSshKey()
    val gpgkey = getArmoredPublicGpgKey()
    return Payload(SshKey(sshkey, "test"), GpgKey(gpgkey))
}