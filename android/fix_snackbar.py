import os

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = "SnackbarHost(hostState = snackbarHostState) { data ->"
replacement = "SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(bottom = 100.dp)) { data ->"
content = content.replace(target, replacement)

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(content)
