package com.music.smb

data class SmbConfig(
    val host: String,
    val port: Int = 445,
    val shareName: String,
    val basePath: String = "",      // subfolder inside the share, "" for share root
    val username: String = "",       // "" for guest/anonymous
    val password: String = "",
    val domain: String = ""
) {
    val isConfigured: Boolean get() = host.isNotBlank() && shareName.isNotBlank()
}