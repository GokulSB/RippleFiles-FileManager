import java.io.BufferedReader
import java.io.InputStreamReader

fun main() {
    val location = "/sdcard/Android/data"
    val cmd = arrayOf("sh", "-c", "stat -c '%F|%n|%s|%Y' \"$location\"/* \"$location\"/.* 2>/dev/null")
    val process = Runtime.getRuntime().exec(cmd)
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    var line: String?
    var count = 0
    while (reader.readLine().also { line = it } != null) {
        println(line)
        count++
        if (count > 5) break
    }
    process.destroy()
}
main()
