package com.ripple.filemanager.data.webdav

import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.InputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/** A file or folder entry returned by a WebDAV PROPFIND, in Ripple Files' own shape. */
data class WebDavEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val contentType: String?
)

sealed class WebDavException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class AuthFailed(cause: Throwable? = null) : WebDavException("Incorrect username or password", cause)
    class NotFound(path: String) : WebDavException("Not found: $path")
    class ServerUnreachable(cause: Throwable? = null) : WebDavException("Couldn't reach the server", cause)
    class Unknown(cause: Throwable? = null) : WebDavException(cause?.message ?: "Something went wrong", cause)
}

/**
 * Thin coroutine-friendly wrapper around Sardine. One instance per connection —
 * construct it from a saved WebDavConnection and hold onto it for the session
 * rather than rebuilding per call.
 */
class WebDavClient(
    private val connection: WebDavConnection,
    private val password: String
) {

    private val sardine: OkHttpSardine by lazy {
        val client = OkHttpClient.Builder().apply {
            if (connection.allowSelfSignedCert) {
                val trustAll = object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
                val sslContext = SSLContext.getInstance("TLS").apply {
                    init(null, arrayOf<TrustManager>(trustAll), SecureRandom())
                }
                sslSocketFactory(sslContext.socketFactory, trustAll)
                hostnameVerifier { _, _ -> true }
            }
        }.build()

        OkHttpSardine(client).apply {
            setCredentials(connection.username, password)
        }
    }

    private fun resolve(path: String): String =
        connection.normalizedUrl + path.trimStart('/')

    private fun <T> wrapErrors(block: () -> T): T = try {
        block()
    } catch (e: com.thegrizzlylabs.sardineandroid.impl.SardineException) {
        when (e.statusCode) {
            401, 403 -> throw WebDavException.AuthFailed(e)
            404 -> throw WebDavException.NotFound(e.message ?: "")
            else -> throw WebDavException.Unknown(e)
        }
    } catch (e: java.io.IOException) {
        throw WebDavException.ServerUnreachable(e)
    } catch (e: Exception) {
        throw WebDavException.Unknown(e)
    }

    /** Verifies the connection actually works before it's saved. */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        wrapErrors { sardine.list(connection.normalizedUrl) }
        true
    }

    suspend fun list(path: String = ""): List<WebDavEntry> = withContext(Dispatchers.IO) {
        wrapErrors {
            val resources: List<DavResource> = sardine.list(resolve(path))
            // Sardine includes the requested folder itself as the first entry — drop it.
            resources.drop(1).map { it.toWebDavEntry() }
        }
    }

    suspend fun download(remotePath: String, destination: File): File = withContext(Dispatchers.IO) {
        wrapErrors {
            sardine.get(resolve(remotePath)).use { input: InputStream ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            destination
        }
    }

    suspend fun upload(localFile: File, remotePath: String, mimeType: String = "application/octet-stream") =
        withContext(Dispatchers.IO) {
            wrapErrors {
                sardine.put(resolve(remotePath), localFile, mimeType)
            }
        }

    suspend fun delete(remotePath: String) = withContext(Dispatchers.IO) {
        wrapErrors { sardine.delete(resolve(remotePath)) }
    }

    suspend fun createFolder(remotePath: String) = withContext(Dispatchers.IO) {
        wrapErrors { sardine.createDirectory(resolve(remotePath)) }
    }

    suspend fun move(fromPath: String, toPath: String) = withContext(Dispatchers.IO) {
        wrapErrors { sardine.move(resolve(fromPath), resolve(toPath)) }
    }

    private fun DavResource.toWebDavEntry(): WebDavEntry = WebDavEntry(
        name = name ?: href.toString().trimEnd('/').substringAfterLast('/'),
        path = href.toString(),
        isDirectory = isDirectory,
        sizeBytes = contentLength ?: 0L,
        lastModifiedMillis = modified?.time ?: 0L,
        contentType = contentType
    )
}
