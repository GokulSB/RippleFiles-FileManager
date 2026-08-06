import re

# Update AppAction.kt
with open('app/src/main/java/com/ripple/filemanager/AppAction.kt', 'r', encoding='utf-8') as f:
    c = f.read()

c = c.replace('object ToggleTextDecoration : AppAction()', 'data class ToggleTextDecoration(val decoration: String) : AppAction()')
with open('app/src/main/java/com/ripple/filemanager/AppAction.kt', 'w', encoding='utf-8') as f:
    f.write(c)

# Update SiftApp.kt
with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    c = f.read()

c = c.replace('onTextDecorationToggle = { onAction(AppAction.ToggleTextDecoration) },', 'onTextDecorationToggle = { onAction(AppAction.ToggleTextDecoration(it)) },')
with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(c)

# Update MainActivity.kt
with open('app/src/main/java/com/ripple/filemanager/MainActivity.kt', 'r', encoding='utf-8') as f:
    c = f.read()

c = c.replace('is AppAction.ToggleTextDecoration -> viewModel.toggleTextDecoration()', 'is AppAction.ToggleTextDecoration -> viewModel.toggleTextDecoration(action.decoration)')
with open('app/src/main/java/com/ripple/filemanager/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(c)
