# FTP / SFTP integration

Drop `ftp/` and `sftp/` into `data/`, so you get `data/ftp/` and `data/sftp/`
alongside `data/smb/`.

## 1. Gradle dependencies

```kotlin
dependencies {
    // FTP
    implementation("commons-net:commons-net:3.11.1")

    // SFTP
    implementation("com.hierynomus:sshj:0.38.0")

    // Both Store classes use kotlinx.serialization — skip if SmbStore already
    // has a different persistence approach and you'd rather match that instead.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
```

If SmbStore does NOT use kotlinx.serialization (e.g. it hand-rolls JSON with
`org.json`, or stores each field as its own SharedPreferences key), tell me
and I'll rewrite FtpStore/SftpStore to match that pattern instead — better to
have one persistence style across all three providers than three different ones.

## 2. Wiring the two buttons in Connections

Your screenshot shows FTP and SFTP as buttons next to LAN/SMB. Whatever click
handler LAN/SMB uses to open its "add connection" dialog, duplicate it for
each:

```kotlin
// wherever the SMB button's onClick lives
FtpButton -> viewModel.onAddFtpConnectionClicked()   // opens Ftp connection dialog
SftpButton -> viewModel.onAddSftpConnectionClicked() // opens Sftp connection dialog
```

Each dialog needs: name, host, port (default 21 / 22), username, password,
and for SFTP an auth-type toggle (password vs. key). On submit:

```kotlin
fun onFtpConnectionSaved(connection: FtpConnection) {
    viewModelScope.launch {
        ftpProvider.connect(connection)
            .onSuccess {
                ftpStore.save(connection)
                // navigate into the new connection's file listing
            }
            .onFailure { e -> /* show error state, e.g. "Login failed" */ }
    }
}

fun onSftpConnectionSaved(connection: SftpConnection) {
    viewModelScope.launch {
        sftpProvider.connect(connection)
            .onSuccess { result ->
                // first-time connect: pin the host key you just saw
                val toSave = if (connection.hostKeyFingerprint == null)
                    connection.copy(hostKeyFingerprint = result.observedFingerprint)
                else connection
                sftpStore.save(toSave)
                // navigate into the new connection's file listing
            }
            .onFailure { e -> /* show error state */ }
    }
}
```

## 3. Routing in MainViewModel / FileRepository

Match the existing `smb_` string-prefix pattern:

```kotlin
when {
    location.startsWith("smb_")  -> smbStorageProvider.listFiles(...)
    location.startsWith("ftp_")  -> ftpStorageProvider.listFiles(strippedPath)
    location.startsWith("sftp_") -> sftpStorageProvider.listFiles(strippedPath)
    location.startsWith("drive_id:") -> fileRepository.listDriveFiles(...)
    ...
}
```

Suggested location id format so each entry in the drawer/home screen maps
back to a saved connection: `"ftp_${connection.id}:${remotePath}"` and
`"sftp_${connection.id}:${remotePath}"` — parse out the id to look up the
`FtpConnection`/`SftpConnection` via `FtpStore.getById()` / `SftpStore.getById()`,
then reuse an already-connected provider instance or reconnect if it dropped.

One thing worth deciding now: SMB being a `class` per-connection instance vs.
a single provider instance you reconnect for each session. FTP/SFTP as written
above are single-instance providers holding one active connection — if a user
can have multiple FTP/SFTP connections open at once (browsing two servers in
split view, etc.), you'll want a small `Map<String, FtpStorageProvider>` keyed
by connection id in the repository rather than one shared instance. Say the
word if that's the case and I'll adjust.

## 4. Permissions / network security config

Nothing extra needed for cleartext FTP beyond internet permission — but if you
have a `networkSecurityConfig` restricting cleartext traffic (common if you
locked this down for the SMB/LAN work), FTP (non-FTPS) traffic to arbitrary
hosts will need an exception, since FTP itself is cleartext by nature. SFTP
rides over SSH so it's unaffected either way.
