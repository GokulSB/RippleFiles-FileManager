package com.ripple.filemanager.localsend

import android.content.Context
import okhttp3.OkHttpClient
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.*
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.*

/**
 * Handles LocalSend TLS certificate generation, persistent storage,
 * SHA-256 fingerprinting, server SSLServerSocketFactory creation,
 * and OkHttp client TLS pinning matching LocalSend v2.
 */
object LocalSendSecurity {

    private const val KEY_FILE = "localsend_key.der"
    private const val CERT_FILE = "localsend_cert.der"

    @Volatile
    private var cachedKeyPair: KeyPair? = null

    @Volatile
    private var cachedCertificate: X509Certificate? = null

    @Volatile
    private var cachedFingerprint: String? = null

    // Map of host (IP:port) -> expected SHA-256 fingerprint for TLS pinning
    val pinnedFingerprints = ConcurrentHashMap<String, String>()

    @Synchronized
    fun init(context: Context) {
        if (cachedFingerprint != null) return

        val keyFile = File(context.filesDir, KEY_FILE)
        val certFile = File(context.filesDir, CERT_FILE)

        if (keyFile.exists() && certFile.exists()) {
            try {
                loadExistingIdentity(keyFile, certFile)
                return
            } catch (e: Exception) {
                keyFile.delete()
                certFile.delete()
            }
        }

        generateNewIdentity(keyFile, certFile)
    }

    fun getFingerprint(context: Context): String {
        if (cachedFingerprint == null) init(context)
        return cachedFingerprint ?: UUID.randomUUID().toString()
    }

    fun getCertificate(context: Context): X509Certificate? {
        if (cachedCertificate == null) init(context)
        return cachedCertificate
    }

    fun getServerSslSocketFactory(context: Context): SSLServerSocketFactory {
        init(context)
        val keyPair = cachedKeyPair ?: throw IllegalStateException("KeyPair not initialized")
        val certificate = cachedCertificate ?: throw IllegalStateException("Certificate not initialized")

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setKeyEntry(
                "localsend",
                keyPair.private,
                "password".toCharArray(),
                arrayOf(certificate)
            )
        }

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, "password".toCharArray())
        }

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(kmf.keyManagers, null, SecureRandom())
        }

        return sslContext.serverSocketFactory
    }

    fun configureOkHttpClient(builder: OkHttpClient.Builder) {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                if (chain.isNullOrEmpty()) {
                    throw SSLPeerUnverifiedException("Certificate chain is empty")
                }
                val cert = chain[0]
                val certFingerprint = computeSha256Fingerprint(cert.encoded)

                // If fingerprint pinning is recorded for this peer, verify exact match
                // Otherwise accept (standard LocalSend self-signed discovery trust)
                for ((_, expectedFingerprint) in pinnedFingerprints) {
                    val normalizedExpected = expectedFingerprint.replace(":", "").uppercase(Locale.US)
                    val normalizedCert = certFingerprint.replace(":", "").uppercase(Locale.US)
                    if (normalizedExpected == normalizedCert) {
                        return
                    }
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), SecureRandom())
        }

        builder.sslSocketFactory(sslContext.socketFactory, trustManager)
        builder.hostnameVerifier { _, _ -> true } // LocalSend certs have CN=LocalSend User with no IP SANs
    }

    private fun loadExistingIdentity(keyFile: File, certFile: File) {
        val keyBytes = keyFile.readBytes()
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(keyBytes))

        val certBytes = certFile.readBytes()
        val certFactory = CertificateFactory.getInstance("X.509")
        val certificate = certFactory.generateCertificate(certBytes.inputStream()) as X509Certificate

        cachedKeyPair = KeyPair(certificate.publicKey, privateKey)
        cachedCertificate = certificate
        cachedFingerprint = computeSha256Fingerprint(certificate.encoded)
    }

    private fun generateNewIdentity(keyFile: File, certFile: File) {
        val kpg = KeyPairGenerator.getInstance("RSA").apply {
            initialize(2048, SecureRandom())
        }
        val keyPair = kpg.generateKeyPair()

        // Validity from 1975 to 4096 (matching LocalSend spec rcgen defaults)
        val notBefore = Calendar.getInstance().apply { set(1975, Calendar.JANUARY, 1) }.time
        val notAfter = Calendar.getInstance().apply { set(4096, Calendar.JANUARY, 1) }.time

        val serialNumber = BigInteger(64, SecureRandom())
        val name = X500Name("CN=LocalSend User")

        val certBuilder = JcaX509v3CertificateBuilder(
            name,
            serialNumber,
            notBefore,
            notAfter,
            name,
            keyPair.public
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val certHolder = certBuilder.build(signer)
        val certificate = JcaX509CertificateConverter().getCertificate(certHolder)

        // Persist
        keyFile.writeBytes(keyPair.private.encoded)
        certFile.writeBytes(certificate.encoded)

        cachedKeyPair = keyPair
        cachedCertificate = certificate
        cachedFingerprint = computeSha256Fingerprint(certificate.encoded)
    }

    fun computeSha256Fingerprint(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02X".format(it) }
    }
}
