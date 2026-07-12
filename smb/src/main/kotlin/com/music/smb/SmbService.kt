package com.music.smb

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig as SmbjConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.EnumSet
import java.util.concurrent.atomic.AtomicReference

/**
 * Locates and streams music files from an SMB/CIFS shared folder, matched
 * via YouTube video ID filename, e.g. "aEAhZpF-bCA.flac".
 */
object SmbService {

    // Extensions to probe, in preference order (lossless first).
    private val CANDIDATE_EXTENSIONS = listOf("flac", "wav", "alac", "m4a", "opus", "mp3")

    @Volatile
    private var config: SmbConfig? = null

    private val client = SMBClient(SmbjConfig.createDefaultConfig())
    private val connectionRef = AtomicReference<Connection?>()
    private val sessionRef = AtomicReference<Session?>()

    fun configure(config: SmbConfig) {
        this.config = config
        // Force reconnect on next use if the target changed.
        disconnect()
    }

    fun isConfigured(): Boolean = config?.isConfigured == true

    suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            getShare()
            Unit
        }.onFailure {
            disconnect()
        }
    }

    @Synchronized
    private fun getShare(): DiskShare {
        val cfg = config ?: throw IllegalStateException("SMB not configured")

        val connection = connectionRef.get()?.takeIf { it.isConnected }
            ?: client.connect(cfg.host, cfg.port).also { connectionRef.set(it) }

        val session = sessionRef.get()?.takeIf { it.connection.isConnected }
            ?: run {
                val auth = if (cfg.username.isBlank()) {
                    AuthenticationContext.anonymous()
                } else {
                    AuthenticationContext(cfg.username, cfg.password.toCharArray(), cfg.domain)
                }
                connection.authenticate(auth).also { sessionRef.set(it) }
            }

        return session.connectShare(cfg.shareName) as DiskShare
    }

    private fun remotePath(fileName: String): String {
        val base = config?.basePath.orEmpty().trim('\\', '/')
        return if (base.isEmpty()) fileName else "$base\\$fileName"
    }

    /**
     * Finds the file on the share whose name (without extension) matches
     * [videoId] exactly, trying [CANDIDATE_EXTENSIONS] in order.
     *
     * @return the matched remote path (relative to the share root) and the
     *         extension found, or null if nothing matched.
     */
    suspend fun findByVideoId(videoId: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        runCatching {
            val share = getShare()
            for (ext in CANDIDATE_EXTENSIONS) {
                val fileName = "$videoId.$ext"
                val path = remotePath(fileName)
                if (share.fileExists(path)) {
                    return@withContext path to ext
                }
            }
            null
        }.getOrElse {
            disconnect()
            null
        }
    }

    /**
     * Opens the remote file and copies it into [destination]. Returns true on success.
     * Used to materialize a local file:// URI for Media3 playback / downloads.
     */
    suspend fun copyToLocal(remotePath: String, destination: OutputStream): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val share = getShare()
                val file: File = share.openFile(
                    remotePath,
                    EnumSet.of(AccessMask.GENERIC_READ),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OPEN,
                    null
                )
                file.use { f ->
                    f.inputStream.use { input ->
                        input.copyTo(destination)
                    }
                }
                true
            }.getOrElse {
                disconnect()
                false
            }
        }

    @Synchronized
    fun disconnect() {
        runCatching { sessionRef.getAndSet(null)?.close() }
        runCatching { connectionRef.getAndSet(null)?.close() }
    }
}
