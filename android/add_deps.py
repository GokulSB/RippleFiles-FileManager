import re

with open('app/build.gradle', 'r', encoding='utf-8') as f:
    content = f.read()

new_deps = '''    implementation 'io.coil-kt:coil-video:2.6.0'
    
    // Shizuku
    def shizuku_version = '13.1.5'
    implementation "dev.rikka.shizuku:api:\"
    implementation "dev.rikka.shizuku:provider:\"
    
    // libsu
    def libsu_version = '6.0.0'
    implementation "com.github.topjohnwu.libsu:core:\"
    implementation "com.github.topjohnwu.libsu:io:\"
}'''
content = content.replace("    implementation 'io.coil-kt:coil-video:2.6.0'\n}", new_deps)

with open('app/build.gradle', 'w', encoding='utf-8') as f:
    f.write(content)
