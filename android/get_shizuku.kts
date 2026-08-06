import java.io.*
import java.net.URL
import java.util.zip.ZipInputStream

fun printShizukuMethods() {
    // Shizuku api jar download
    val url = URL("https://repo1.maven.org/maven2/dev/rikka/shizuku/api/13.1.5/api-13.1.5-sources.jar")
    val conn = url.openConnection()
    val zis = ZipInputStream(conn.getInputStream())
    var entry = zis.nextEntry
    while (entry != null) {
        if (entry.name.endsWith("Shizuku.java")) {
            val bytes = zis.readBytes()
            val text = String(bytes)
            for (line in text.split("\n")) {
                if (line.contains("newProcess") || line.contains("execute")) {
                    println(line)
                }
            }
        }
        entry = zis.nextEntry
    }
    zis.close()
}

printShizukuMethods()
