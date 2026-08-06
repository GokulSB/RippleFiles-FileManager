import os

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = "if (!viewModel.hasRestrictedAccess(state.location)) {"
replacement = '''if (!viewModel.hasRestrictedAccess(state.location)) {
                      androidx.compose.runtime.LaunchedEffect(state.location) {
                          viewModel.autoRequestAccess(state.location)
                      }'''

if "LaunchedEffect(state.location)" not in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Replaced")
else:
    print("Already exists")
