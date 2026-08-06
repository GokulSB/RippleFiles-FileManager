import os

with open('app/src/main/java/com/ripple/filemanager/MainViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix safRequestEvent cast
old_saf = '''    val safRequestEvent: kotlinx.coroutines.flow.SharedFlow<String> = kotlinx.coroutines.flow.asSharedFlow(_safRequestEvent)'''
new_saf = '''    val safRequestEvent: kotlinx.coroutines.flow.SharedFlow<String> = _safRequestEvent'''
content = content.replace(old_saf, new_saf)

with open('app/src/main/java/com/ripple/filemanager/MainViewModel.kt', 'w', encoding='utf-8') as f:
    f.write(content)
