import java.io.*
import java.net.URL
import java.util.zip.ZipInputStream

fun downloadLibsu() {
    // Download libsu core sources jar
    val url = URL("https://repo1.maven.org/maven2/com/github/topjohnwu/libsu/core/6.0.0/core-6.0.0-sources.jar")
    val conn = url.openConnection()
    val zis = ZipInputStream(conn.getInputStream())
    var entry = zis.nextEntry
    while (entry != null) {
        if (entry.name.contains("Shell.java")) {
            val bytes = zis.readBytes()
            val text = String(bytes)
            val builderSection = text.substringAfter("class Builder").substringBefore("public Shell build()")
            println(builderSection)
        }
        entry = zis.nextEntry
    }
    zis.close()
}

downloadLibsu()
