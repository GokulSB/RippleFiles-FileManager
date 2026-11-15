package com.ripple.filemanager.archive

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ripple.filemanager.archive.engine.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ArchiveEngineParityTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val fixturesDir = File("/data/local/tmp/fixtures")
    private lateinit var legacyEngine: LegacyEngine
    private lateinit var sevenZipEngine: SevenZipEngine
    private lateinit var workDir: File

    @Before
    fun setUp() {
        legacyEngine = ArchiveEngineManager.getLegacyEngine()
        sevenZipEngine = ArchiveEngineManager.getSevenZipEngine(context)
        workDir = File(context.cacheDir, "parity_test_${System.currentTimeMillis()}").apply { mkdirs() }
    }

    @Test
    fun testPlainZipParity() = runBlocking {
        val plainZip = File(fixturesDir, "parity_plain.zip")
        assertTrue("parity_plain.zip exists", plainZip.exists())

        // 1. Listing Parity
        val legacyEntries = legacyEngine.list(plainZip)
        val sevenZipEntries = sevenZipEngine.list(plainZip)

        assertEquals("Entry count matches", 3, legacyEntries.size)
        assertEquals("SevenZip entry count matches", 3, sevenZipEntries.size)

        val legacyPaths = legacyEntries.map { it.path.replace('\\', '/') }.sorted()
        val sevenZipPaths = sevenZipEntries.map { it.path.replace('\\', '/') }.sorted()
        assertEquals("Paths match", legacyPaths, sevenZipPaths)

        // 2. Extraction Parity
        val legacyOut = File(workDir, "legacy_plain").apply { mkdirs() }
        val sevenZipOut = File(workDir, "sevenzip_plain").apply { mkdirs() }

        val legacyResults = legacyEngine.extract(plainZip, legacyOut).toList()
        assertTrue("Legacy extract succeeded", legacyResults.any { it is EngineProgress.Success })

        val sevenZipResults = sevenZipEngine.extract(plainZip, sevenZipOut).toList()
        assertTrue("SevenZip extract succeeded", sevenZipResults.any { it is EngineProgress.Success })

        // 3. Compare decompressed contents byte-for-byte
        for (path in legacyPaths) {
            val f1 = File(legacyOut, path)
            val f2 = File(sevenZipOut, path)
            assertTrue("File exists in legacy: $path", f1.exists())
            assertTrue("File exists in sevenzip: $path", f2.exists())
            assertEquals("Content matches for $path", f1.readText(), f2.readText())
        }
    }

    @Test
    fun testZipSlipDefense() = runBlocking {
        val zipSlip = File(fixturesDir, "parity_zipslip.zip")
        assertTrue("parity_zipslip.zip exists", zipSlip.exists())

        val destDir = File(workDir, "zipslip_out").apply { mkdirs() }
        val results = sevenZipEngine.extract(zipSlip, destDir).toList()

        // Verify extraction error was reported due to security violation
        assertTrue("Extraction blocked or reported error", results.any { it is EngineProgress.Error })

        // Verify malicious traversal files DO NOT exist outside destDir
        val parent = destDir.parentFile!!
        val evilFile = File(parent, "evil_escape.txt")
        assertFalse("Zip-slip evil_escape.txt was blocked", evilFile.exists())

        val rootEscape = File("/root_escape.txt")
        assertFalse("Zip-slip root_escape.txt was blocked", rootEscape.exists())
    }

    @Test
    fun testSymlinkEscapeDefense() = runBlocking {
        val symlinkTar = File(fixturesDir, "malicious_symlink.tar")
        if (symlinkTar.exists()) {
            val destDir = File(workDir, "symlink_out").apply { mkdirs() }
            sevenZipEngine.extract(symlinkTar, destDir).toList()

            // Verify safe file extracted
            val safeFile = File(destDir, "safe.txt")
            assertTrue("safe.txt extracted", safeFile.exists())

            // Verify malicious symlink did NOT escape to /etc/passwd
            val evilEscape = File(destDir.parentFile, "etc/passwd")
            assertFalse("Malicious symlink escape was blocked", evilEscape.exists())
        }
    }

    @Test
    fun testUnicodeAndEmojiZip() = runBlocking {
        val unicodeZip = File(fixturesDir, "unicode_emoji.zip")
        if (unicodeZip.exists()) {
            val entries = sevenZipEngine.list(unicodeZip)
            assertTrue("Unicode entries found", entries.isNotEmpty())

            val destDir = File(workDir, "unicode_zip_out").apply { mkdirs() }
            val results = sevenZipEngine.extract(unicodeZip, destDir).toList()
            assertTrue("Extract succeeded", results.any { it is EngineProgress.Success })

            // Check emoji and unicode files extracted
            val emojiFile = File(destDir, "🎉_celebrate_🚀.txt")
            assertTrue("Emoji filename extracted intact", emojiFile.exists())

            val deepFile = File(destDir, "nested/level1/level2/deep.txt")
            assertTrue("Deep nested file extracted intact", deepFile.exists())
        }
    }

    @Test
    fun testUnicodeAndEmojiTarGz() = runBlocking {
        val unicodeTarGz = File(fixturesDir, "unicode_emoji.tar.gz")
        if (unicodeTarGz.exists()) {
            val destDir = File(workDir, "unicode_targz_out").apply { mkdirs() }
            val results = sevenZipEngine.extract(unicodeTarGz, destDir).toList()
            assertTrue("Extract tar.gz succeeded", results.any { it is EngineProgress.Success })
        }
    }

    @Test
    fun testTarBz2AndTarXz() = runBlocking {
        val sampleBz2 = File(fixturesDir, "sample.tar.bz2")
        if (sampleBz2.exists()) {
            val destDir = File(workDir, "bz2_out").apply { mkdirs() }
            val results = sevenZipEngine.extract(sampleBz2, destDir).toList()
            assertTrue("Extract tar.bz2 succeeded", results.any { it is EngineProgress.Success })
        }

        val sampleXz = File(fixturesDir, "sample.tar.xz")
        if (sampleXz.exists()) {
            val destDir = File(workDir, "xz_out").apply { mkdirs() }
            val results = sevenZipEngine.extract(sampleXz, destDir).toList()
            assertTrue("Extract tar.xz succeeded", results.any { it is EngineProgress.Success })
        }
    }

    @Test
    fun testSingleStreamsGzBz2Xz() = runBlocking {
        for (ext in listOf("gz", "bz2", "xz")) {
            val f = File(fixturesDir, "single_stream.$ext")
            if (f.exists()) {
                val destDir = File(workDir, "stream_${ext}_out").apply { mkdirs() }
                val results = sevenZipEngine.extract(f, destDir).toList()
                assertTrue("Extract single stream $ext succeeded", results.any { it is EngineProgress.Success })
            }
        }
    }

    @Test
    fun testZip64Archive() = runBlocking {
        val zip64 = File(fixturesDir, "sample_zip64.zip")
        if (zip64.exists()) {
            val entries = sevenZipEngine.list(zip64)
            assertTrue("Zip64 entries listed", entries.isNotEmpty())
            val destDir = File(workDir, "zip64_out").apply { mkdirs() }
            val results = sevenZipEngine.extract(zip64, destDir).toList()
            assertTrue("Zip64 extract succeeded", results.any { it is EngineProgress.Success })
        }
    }

    @Test
    fun test10kSmallFilesStreaming() = runBlocking {
        val tenkZip = File(fixturesDir, "parity_10k.zip")
        assertTrue("parity_10k.zip exists", tenkZip.exists())

        val entries = sevenZipEngine.list(tenkZip)
        assertEquals("10,000 files listed without OOM", 10000, entries.size)
    }

    @Test
    fun testAdvancedFormatsSevenZip() = runBlocking {
        // 1. 7z format
        val sample7z = File(fixturesDir, "sample.7z")
        if (sample7z.exists()) {
            val entries7z = sevenZipEngine.list(sample7z)
            assertTrue("sample.7z listed entries", entries7z.isNotEmpty())
            val out7z = File(workDir, "out_7z").apply { mkdirs() }
            val res = sevenZipEngine.extract(sample7z, out7z).toList()
            assertTrue("7z extract succeeded", res.any { it is EngineProgress.Success })
        }

        // 2. zstd format
        val sampleZst = File(fixturesDir, "sample.zst")
        if (sampleZst.exists()) {
            val outZst = File(workDir, "out_zst").apply { mkdirs() }
            val res = sevenZipEngine.extract(sampleZst, outZst).toList()
            assertTrue("zst extract succeeded", res.any { it is EngineProgress.Success })
        }

        // 3. iso format
        val sampleIso = File(fixturesDir, "sample.iso")
        if (sampleIso.exists()) {
            val entriesIso = sevenZipEngine.list(sampleIso)
            assertTrue("sample.iso listed entries", entriesIso.isNotEmpty())
        }
    }

    @Test
    fun testCreateSingleStreamAutoTarWrap() = runBlocking {
        val testFiles = listOf(
            File(workDir, "doc1.txt").apply { writeText("Hello World from doc1") },
            File(workDir, "doc2.txt").apply { writeText("Hello World from doc2") }
        )

        // Test tar.gz creation with auto-tar wrapping of multiple files
        val outTarGz = File(workDir, "archive.tar.gz")
        val createResults = sevenZipEngine.create(
            inputs = testFiles,
            outFile = outTarGz,
            options = CreateOptions(format = "tar.gz", level = 5)
        ).toList()

        assertTrue("tar.gz create succeeded", createResults.any { it is EngineProgress.Success })
        assertTrue("tar.gz output file exists", outTarGz.exists())
        assertTrue("tar.gz size > 0", outTarGz.length() > 0)

        // Verify unpacking
        val unpackDir = File(workDir, "unpacked_tar_gz").apply { mkdirs() }
        sevenZipEngine.extract(outTarGz, unpackDir).toList()

        // Gzip produces an intermediate tar, which can be extracted or inspected
        val extractedFiles = unpackDir.walkTopDown().filter { it.isFile }.toList()
        assertTrue("Unpacked contents exist", extractedFiles.isNotEmpty())
    }

    @Test
    fun testArchiveIntegrityTest() = runBlocking {
        val plainZip = File(fixturesDir, "parity_plain.zip")
        val testResults = sevenZipEngine.test(plainZip).toList()
        assertTrue("Integrity test passed", testResults.any { it is EngineProgress.Success })
        assertFalse("Integrity test had no errors", testResults.any { it is EngineProgress.Error })
    }
}
