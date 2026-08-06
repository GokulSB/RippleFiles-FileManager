import re
with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    c = f.read()

c = c.replace('onAction(AppAction.SetErrorMessage("Permission Denied: You MUST check the box to allow Sift to view your Google Drive files. Please try signing in again.")\n', 
              'onAction(AppAction.SetErrorMessage("Permission Denied: You MUST check the box to allow Sift to view your Google Drive files. Please try signing in again."))\n')
c = c.replace('onAction(AppAction.SetErrorMessage("Google Login Failed: ${e.message}")\n', 
              'onAction(AppAction.SetErrorMessage("Google Login Failed: ${e.message}"))\n')

c = c.replace('onAction(AppAction.RenameFile(file, newName))', 'onAction(AppAction.RenameFile(file.path, newName))')

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(c)
