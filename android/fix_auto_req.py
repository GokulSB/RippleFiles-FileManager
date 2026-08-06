import os

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_block = '''              } else if (files.isEmpty() && !state.isLoading) {
                  if (!viewModel.hasRestrictedAccess(state.location)) {
                      Column('''

new_block = '''              } else if (files.isEmpty() && !state.isLoading) {
                  if (!viewModel.hasRestrictedAccess(state.location)) {
                      androidx.compose.runtime.LaunchedEffect(state.location) {
                          viewModel.autoRequestAccess(state.location)
                      }
                      Column('''

content = content.replace(old_block, new_block)

if "LaunchedEffect(state.location) {" not in content:
    print("Could not find block to replace")

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(content)
