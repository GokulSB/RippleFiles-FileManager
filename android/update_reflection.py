import os

filepath = 'app/src/main/java/com/ripple/filemanager/FileRepository.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

execute_shizuku = """    fun executeShizukuCommand(cmd: String): Boolean {
        if (!hasShizuku()) return false
        return try {
            val m = rikka.shizuku.Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            m.isAccessible = true
            val process = m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }"""

content = content.replace("""    fun executeShizukuCommand(cmd: String): Boolean {
        if (!hasShizuku()) return false
        return try {
            val process = rikka.shizuku.Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }""", execute_shizuku)

content = content.replace("""val process = rikka.shizuku.Shizuku.newProcess(arrayOf("sh", "-c", "stat -c '%F|%n|%s|%Y' \\"\\\\"/* \\"\\\\"/.* 2>/dev/null"), null, null)""", """val m = rikka.shizuku.Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                m.isAccessible = true
                val process = m.invoke(null, arrayOf("sh", "-c", "stat -c '%F|%n|%s|%Y' \\"\\\\"/* \\"\\\\"/.* 2>/dev/null"), null, null) as Process""")

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print("Applied reflection for Shizuku")
