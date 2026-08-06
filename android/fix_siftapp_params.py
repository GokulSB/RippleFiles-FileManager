import re

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    c = f.read()

# Fix SiftApp signature
c = c.replace('fun SiftApp(\n    state: AppState,\n    onAction: (AppAction) -> Unit,\n    windowWidthSizeClass: WindowWidthSizeClass\n) {',
              'fun SiftApp(\n    state: AppState,\n    onAction: (AppAction) -> Unit,\n    snackbarHostState: androidx.compose.material3.SnackbarHostState,\n    windowWidthSizeClass: androidx.compose.material3.windowsizeclass.WindowWidthSizeClass\n) {')

# Fix MainContent calls
c = c.replace('viewModel = viewModel,', 'state = state,\n                        onAction = onAction,\n                        snackbarHostState = snackbarHostState,')

# Fix onLocationSelected
c = c.replace('onLocationSelected = viewModel::setLocation,', 'onLocationSelected = { onAction(AppAction.SetLocation(it)) },')

# Fix onClose
c = c.replace('onClose = viewModel::clearSelection,', 'onClose = { onAction(AppAction.ClearSelection) },')

# The compile error at line 607, 608, 610:
# e: file:///D:/File%20Manager/android/app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt:607:49 @Composable invocations can only happen from the context of a @Composable function
# 607 is probably: leadingIcon = { Icon(...) }
# Let's check why those are failing. They are probably inside SearchBar which was refactored.
# Actually, the error is inside SiftApp for the TopBar search field.
# Let's just fix the known errors first.

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(c)

