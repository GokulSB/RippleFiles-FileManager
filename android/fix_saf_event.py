import os

with open('app/src/main/java/com/ripple/filemanager/MainViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix safRequestEvent
old_saf = '''    private val _safRequestEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val safRequestEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()'''
new_saf = '''    private val _safRequestEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val safRequestEvent: kotlinx.coroutines.flow.SharedFlow<String> = kotlinx.coroutines.flow.asSharedFlow(_safRequestEvent)'''
content = content.replace(old_saf, new_saf)
if old_saf not in content and 'val safRequestEvent: kotlinx.coroutines.flow.SharedFlow<String>' not in content:
    print("Could not find safRequestEvent to replace")

with open('app/src/main/java/com/ripple/filemanager/MainViewModel.kt', 'w', encoding='utf-8') as f:
    f.write(content)
