package com.ripple.filemanager.data.webdav

/**
 * A saved WebDAV-family connection. Used for both generic WebDAV servers and
 * Nextcloud instances — Nextcloud is WebDAV underneath, just with a known
 * endpoint convention and app-password support baked into the setup flow.
 */
data class WebDavConnection(
    val id: String,
    val displayName: String,
    /** Full base URL including the DAV path, e.g. https://cloud.example.com/remote.php/dav/files/goku/ */
    val serverUrl: String,
    val username: String,
    
    val isNextcloud: Boolean = false,
    val allowSelfSignedCert: Boolean = false
) {
    init {
        require(serverUrl.startsWith("http://") || serverUrl.startsWith("https://")) {
            "serverUrl must include a scheme"
        }
    }

    /** Guarantees a single trailing slash so path joins in WebDavClient are predictable. */
    val normalizedUrl: String
        get() = serverUrl.trimEnd('/') + "/"
}

/** Builds the standard Nextcloud WebDAV endpoint from just a domain + username. */
fun buildNextcloudUrl(domain: String, username: String): String {
    val scheme = if (domain.startsWith("http://")) "http://" else "https://"
    val cleanDomain = domain
        .removePrefix("https://")
        .removePrefix("http://")
        .trimEnd('/')
    return "$scheme$cleanDomain/remote.php/dav/files/$username/"
}
