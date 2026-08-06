import os

with open('app/build.gradle', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('versionCode 6', 'versionCode 7')
content = content.replace('versionName "1.0.5"', 'versionName "1.0.6"')

with open('app/build.gradle', 'w', encoding='utf-8') as f:
    f.write(content)
