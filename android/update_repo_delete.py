import os

filepath = 'app/src/main/java/com/ripple/filemanager/FileRepository.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

delete_func = """
    fun executeShizukuCommand(cmd: String): Boolean {
        if (!hasShizuku()) return false
        return try {
            val process = rikka.shizuku.Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
    
    fun deleteRestrictedPath(path: String): Boolean {
        if (path.contains("Android/data") || path.contains("Android/obb")) {
            if (hasShizuku()) {
                return executeShizukuCommand("rm -rf \\"\\"")
            }
            if (hasRoot()) {
                return com.topjohnwu.superuser.io.SuFile(path).deleteRecursively()
            }
        }
        return java.io.File(path).deleteRecursively()
    }
    
    fun hasRoot(): Boolean {
"""

content = content.replace('    fun hasRoot(): Boolean {', delete_func)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print("Added deleteRestrictedPath")
