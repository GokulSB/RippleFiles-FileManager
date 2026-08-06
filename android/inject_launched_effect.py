import os

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add LaunchedEffect
old_block = '''                                      val hasAccess = viewModel.hasRestrictedAccess(state.location)
                                      if (!hasAccess) {
                                          Column('''
new_block = '''                                      val hasAccess = viewModel.hasRestrictedAccess(state.location)
                                      if (!hasAccess) {
                                          androidx.compose.runtime.LaunchedEffect(state.location) {
                                              viewModel.autoRequestAccess(state.location)
                                          }
                                          Column('''

content = content.replace(old_block, new_block)

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(content)
