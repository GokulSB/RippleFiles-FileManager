# ============================================================================
# Ripple Files ProGuard & R8 Optimization Rules
# ============================================================================

# General attributes preservation for reflection, line numbers and stack traces
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# Preserve native JNI methods across all packages
-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve Enum values and valueOf methods
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve Parcelable Creator
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Suppress standard JVM/JDK warnings not present on Android runtime
-dontwarn javax.annotation.**
-dontwarn javax.naming.**
-dontwarn javax.xml.**
-dontwarn javax.net.**
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn org.ietf.jgss.**
-dontwarn sun.misc.**
-dontwarn sun.security.**
-dontwarn java.lang.management.**

# ============================================================================
# App Models & Data Classes (Serialized / Deserialized via Gson & State)
# ============================================================================
-keep class com.ripple.filemanager.data.** { *; }
-keep class com.ripple.filemanager.localsend.** { *; }
-keep class com.ripple.filemanager.FileItem { *; }
-keep class com.ripple.filemanager.CleanerData { *; }
-keep class com.ripple.filemanager.StorageCategory { *; }
-keep class com.ripple.filemanager.AppAction** { *; }
-keep class com.ripple.filemanager.AppState** { *; }

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# ============================================================================
# Cryptography & BouncyCastle (TLS, KeyStore, Certificates)
# ============================================================================
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ============================================================================
# LocalSend & Networking (NanoHTTPD, OkHttp3, Okio)
# ============================================================================
-keep class fi.iki.elonen.** { *; }
-dontwarn fi.iki.elonen.**

-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclassmembers class okhttp3.internal.publicsuffix.PublicSuffixDatabase {
    java.lang.String[] PREPARED_PUBLIC_SUFFIX_LIST;
    java.lang.String[] PREPARED_EXCEPTION_LIST;
}

# ============================================================================
# SMB, SFTP & FTP Network Storage Providers
# ============================================================================
# SMB (smbj)
-keep class com.hierynomus.** { *; }
-dontwarn com.hierynomus.**

# SFTP (sshj)
-keep class net.schmizz.sshj.** { *; }
-keep class com.hierynomus.sshj.** { *; }
-dontwarn net.schmizz.sshj.**

# FTP (Apache Commons Net)
-keep class org.apache.commons.net.** { *; }
-dontwarn org.apache.commons.net.**

# WebDAV (Sardine-Android)
-keep class com.thegrizzlylabs.sardineandroid.** { *; }
-dontwarn com.thegrizzlylabs.sardineandroid.**
-keep class org.apache.http.** { *; }
-dontwarn org.apache.http.**

# ============================================================================
# Cloud Providers (Dropbox & Google Drive)
# ============================================================================
-keep class com.dropbox.core.** { *; }
-dontwarn com.dropbox.core.**

-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.api.services.drive.**

# ============================================================================
# Archive Handling (Unrar5j & Zip4j)
# ============================================================================
-keep class be.stef.rar.** { *; }
-dontwarn be.stef.rar.**
-keep class net.lingala.zip4j.** { *; }
-dontwarn net.lingala.zip4j.**

# ============================================================================
# Shizuku & Root Providers
# ============================================================================
-keep class moe.shizuku.** { *; }
-keep class dev.rikka.shizuku.** { *; }
-keep class com.github.topjohnwu.libsu.** { *; }
-dontwarn moe.shizuku.**
-dontwarn dev.rikka.shizuku.**
-dontwarn com.github.topjohnwu.libsu.**

# ============================================================================
# Media, Documents & Images (ExoPlayer, Coil, PDFBox, POI)
# ============================================================================
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

-keep class coil.** { *; }
-dontwarn coil.**

-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**

-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**

# ============================================================================
# Coroutines & Compose Runtime
# ============================================================================
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ============================================================================
# Missing Optional Third-Party Classes (POI, Saxon, Log4j, Swing, OSGi)
# ============================================================================
-dontwarn javax.el.**
-dontwarn javax.swing.**
-dontwarn net.sf.saxon.**
-dontwarn org.osgi.framework.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.apache.logging.log4j.**
-dontwarn net.engio.mbassy.**

