import re

# 1. Add actions to AppAction.kt
with open('app/src/main/java/com/ripple/filemanager/AppAction.kt', 'r', encoding='utf-8') as f:
    app_action = f.read()

cleaner_actions = """
    // Cleaner Screen
    data class SetCleanerCategory(val category: String?) : AppAction()
    data class SelectAllCleanerFiles(val ids: List<Int>) : AppAction()
    object ClearCleanerSelection : AppAction()
    object DeleteSelectedCleanerFiles : AppAction()
    data class ToggleCleanerSelection(val id: Int) : AppAction()
"""
if 'SetCleanerCategory' not in app_action:
    app_action = app_action.replace('// General', cleaner_actions + '\n    // General')
    with open('app/src/main/java/com/ripple/filemanager/AppAction.kt', 'w', encoding='utf-8') as f:
        f.write(app_action)

# 2. Refactor CleanerScreen.kt
with open('app/src/main/java/com/ripple/filemanager/ui/CleanerScreen.kt', 'r', encoding='utf-8') as f:
    cleaner_screen = f.read()

cleaner_screen = cleaner_screen.replace('import com.ripple.filemanager.MainViewModel', 'import com.ripple.filemanager.AppAction\nimport androidx.compose.material3.SnackbarHostState')
cleaner_screen = cleaner_screen.replace('fun CleanerScreen(viewModel: MainViewModel, state: com.ripple.filemanager.AppState)', 'fun CleanerScreen(state: com.ripple.filemanager.AppState, onAction: (AppAction) -> Unit, snackbarHostState: SnackbarHostState)')
cleaner_screen = cleaner_screen.replace('viewModel.setCleanerCategory(null)', 'onAction(AppAction.SetCleanerCategory(null))')
cleaner_screen = cleaner_screen.replace('viewModel.setCleanerCategory(it)', 'onAction(AppAction.SetCleanerCategory(it))')
cleaner_screen = cleaner_screen.replace('viewModel.setCleanerScreenVisible(false)', 'onAction(AppAction.SetCleanerScreenVisible(false))')
cleaner_screen = cleaner_screen.replace('viewModel.selectAllCleanerFiles(catData.files.map { it.id })', 'onAction(AppAction.SelectAllCleanerFiles(catData.files.map { it.id }))')
cleaner_screen = cleaner_screen.replace('viewModel.clearCleanerSelection()', 'onAction(AppAction.ClearCleanerSelection)')
cleaner_screen = cleaner_screen.replace('viewModel.deleteSelectedCleanerFiles()', 'onAction(AppAction.DeleteSelectedCleanerFiles)')
cleaner_screen = cleaner_screen.replace('viewModel.toggleCleanerSelection(it)', 'onAction(AppAction.ToggleCleanerSelection(it))')

with open('app/src/main/java/com/ripple/filemanager/ui/CleanerScreen.kt', 'w', encoding='utf-8') as f:
    f.write(cleaner_screen)
