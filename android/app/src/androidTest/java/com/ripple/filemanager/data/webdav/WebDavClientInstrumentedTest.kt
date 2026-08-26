package com.ripple.filemanager.data.webdav

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ripple.filemanager.BuildConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.util.UUID

/**
 * Instrumented tests that exercise WebDavClient against a REAL server —
 * these are not unit tests, they need network access from the test device.
 *
 * Point TEST_SERVER_URL / TEST_USERNAME / TEST_PASSWORD at a throwaway
 * Nextcloud instance (e.g. `docker run -d -p 8080:80 nextcloud`) or a
 * generic WebDAV server before running. Never commit real credentials here —
 * pull them from local.properties / a gradle property / env var instead of
 * hardcoding, the same way you already do for FTP/SFTP test creds.
 *
 * Run order matters (upload before list/download/delete), hence FixMethodOrder.
 * Each test cleans up after itself where possible so re-runs don't accumulate junk.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class WebDavClientInstrumentedTest {

    companion object {
        private val testFolder = "ripple-test-${UUID.randomUUID()}"
    }

    // TODO: wire these to BuildConfig fields or a gitignored test-secrets.properties
    // instead of leaving literals in source, the same pattern you use for FTP/SFTP.
    private val serverUrl = BuildConfig.TEST_WEBDAV_URL
    private val username = BuildConfig.TEST_WEBDAV_USER
    private val password = BuildConfig.TEST_WEBDAV_PASSWORD
    private val allowSelfSigned = BuildConfig.TEST_WEBDAV_ALLOW_SELF_SIGNED

    
    private lateinit var client: WebDavClient
    private lateinit var connection: WebDavConnection

    @Before
    fun setUp() {
        connection = WebDavConnection(
            id = "test-connection",
            displayName = "Test Server",
            serverUrl = serverUrl,
            username = username,
            allowSelfSignedCert = allowSelfSigned
        )
        // If password now lives outside WebDavConnection per your
        // EncryptedSharedPreferences change, swap this for however
        // WebDavClient/your credential store expects it injected —
        // e.g. WebDavClient(connection, credentialProvider) or similar.
        client = WebDavClient(connection, password)
    }

    @Test
    fun test01_connectionSucceedsWithValidCredentials(): Unit = runBlocking {
        val result = client.testConnection()
        assertTrue("testConnection() should return true for valid credentials", result)
    }

    @Test
    fun test02_connectionFailsWithBadPassword(): Unit = runBlocking {
        val badConnection = connection.copy(id = "bad-creds-test")
        val badClient = WebDavClient(badConnection, "bad-password-123") // adjust if password is injected separately
        try {
            badClient.testConnection()
            fail("Expected WebDavException.AuthFailed for bad credentials")
        } catch (e: WebDavException.AuthFailed) {
            // expected
        }
    }

    @Test
    fun test03_createFolderThenAppearsInList(): Unit = runBlocking {
        client.createFolder(testFolder)
        val entries = client.list("")
        assertTrue(
            "Newly created folder should appear in root listing",
            entries.any { it.name == testFolder && it.isDirectory }
        )
    }

    @Test
    fun test04_uploadThenListShowsFile(): Unit = runBlocking {
        val localFile = File.createTempFile("ripple_upload_test", ".txt").apply {
            writeText("Ripple Files WebDAV round-trip test — ${System.currentTimeMillis()}")
        }

        client.upload(localFile, "$testFolder/round-trip.txt", mimeType = "text/plain")

        val entries = client.list(testFolder)
        val uploaded = entries.firstOrNull { it.name == "round-trip.txt" }
        assertNotNull("Uploaded file should be visible in folder listing", uploaded)
        assertEquals(localFile.length(), uploaded!!.sizeBytes)

        localFile.delete()
    }

    @Test
    fun test05_downloadMatchesUploadedContent(): Unit = runBlocking {
        val originalText = "Round-trip integrity check ${System.currentTimeMillis()}"
        val localUpload = File.createTempFile("ripple_download_source", ".txt").apply {
            writeText(originalText)
        }
        client.upload(localUpload, "$testFolder/integrity.txt", mimeType = "text/plain")

        val downloadTarget = File.createTempFile("ripple_download_target", ".txt")
        client.download("$testFolder/integrity.txt", downloadTarget)

        assertEquals(originalText, downloadTarget.readText())

        localUpload.delete()
        downloadTarget.delete()
    }

    @Test
    fun test06_moveRenamesFileOnServer(): Unit = runBlocking {
        val localFile = File.createTempFile("ripple_move_test", ".txt").apply {
            writeText("move me")
        }
        client.upload(localFile, "$testFolder/before-move.txt", mimeType = "text/plain")
        client.move("$testFolder/before-move.txt", "$testFolder/after-move.txt")

        val entries = client.list(testFolder)
        assertTrue(entries.none { it.name == "before-move.txt" })
        assertTrue(entries.any { it.name == "after-move.txt" })

        localFile.delete()
    }

    @Test
    fun test07_deleteRemovesFileFromServer(): Unit = runBlocking {
        val localFile = File.createTempFile("ripple_delete_test", ".txt").apply {
            writeText("delete me")
        }
        client.upload(localFile, "$testFolder/to-delete.txt", mimeType = "text/plain")
        client.delete("$testFolder/to-delete.txt")

        val entries = client.list(testFolder)
        assertTrue(
            "Deleted file should no longer appear in listing",
            entries.none { it.name == "to-delete.txt" }
        )

        localFile.delete()
    }

    @Test
    fun test08_notFoundThrowsCorrectException(): Unit = runBlocking {
        try {
            client.download("$testFolder/does-not-exist.txt", File.createTempFile("nope", ".txt"))
            fail("Expected WebDavException.NotFound for a missing remote file")
        } catch (e: WebDavException.NotFound) {
            // expected
        }
    }

    @Test
    fun test09_cleanUpTestFolder(): Unit = runBlocking {
        // Best-effort teardown of everything this run created.
        try {
            client.delete("$testFolder/after-move.txt")
        } catch (_: Exception) { /* may not exist depending on which tests ran */ }
        try {
            client.delete(testFolder)
        } catch (e: Exception) {
            // Some servers refuse DELETE on non-empty folders — leaving a stray
            // ripple-test-* folder is harmless but worth noticing if this fires.
            System.err.println("Cleanup warning: could not remove $testFolder — ${e.message}")
        }
    }
}
