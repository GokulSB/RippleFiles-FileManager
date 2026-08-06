import re

with open('settings.gradle', 'r', encoding='utf-8') as f:
    content = f.read()

old_repos = '''    repositories {
        google()
        mavenCentral()
    }'''
new_repos = '''    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }'''
content = content.replace(old_repos, new_repos)

with open('settings.gradle', 'w', encoding='utf-8') as f:
    f.write(content)
