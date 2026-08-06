import os

with open('app/build.gradle', 'r', encoding='utf-8') as f:
    content = f.read()

new_deps = '''
    // Shizuku
    def shizuku_version = '13.1.5'
    implementation "dev.rikka.shizuku:api:\"
    implementation "dev.rikka.shizuku:provider:\"
    
    // libsu
    def libsu_version = '6.0.0'
    implementation "com.github.topjohnwu.libsu:core:\"
    implementation "com.github.topjohnwu.libsu:io:\"
    
    implementation "androidx.documentfile:documentfile:1.0.1"
}'''

content = content.replace("dependencies { implementation 'io.coil-kt:coil-video:2.6.0' }", "dependencies { implementation 'io.coil-kt:coil-video:2.6.0' } \ndependencies {" + new_deps)

with open('app/build.gradle', 'w', encoding='utf-8') as f:
    f.write(content)
