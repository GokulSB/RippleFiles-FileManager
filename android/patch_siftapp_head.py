import os
content = open('android/SiftApp_head.kt', 'r', encoding='utf-8').read()
content = content.replace('com.sift', 'com.ripple')
open('android/app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8').write(content)
