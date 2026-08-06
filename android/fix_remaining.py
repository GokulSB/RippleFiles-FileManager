import re
import os

# Update AppAction.kt
app_action_path = 'app/src/main/java/com/ripple/filemanager/AppAction.kt'
with open(app_action_path, 'r', encoding='utf-8') as f:
    app_action = f.read()

if 'SetThemeMode' not in app_action:
    theme_actions = """
    // Theme & Customization
    data class SetThemeMode(val mode: com.ripple.filemanager.ui.theme.ThemeMode) : AppAction()
    data class SetThemeHue(val hue: Float) : AppAction()
    data class SetDynamicSystemTheme(val dynamic: Boolean) : AppAction()
    data class SetIconShape(val shape: String) : AppAction()
    data class SetFontStyle(val style: String) : AppAction()
    object ToggleTextDecoration : AppAction()
    data class SetMainTextScale(val scale: Float) : AppAction()
    data class SetSubTextScale(val scale: Float) : AppAction()
    data class SetCornerRoundness(val roundness: Float) : AppAction()
"""
    app_action = app_action.replace('// General', theme_actions + '\n    // General')
    with open(app_action_path, 'w', encoding='utf-8') as f:
        f.write(app_action)

# Update MainViewModel.kt
view_model_path = 'app/src/main/java/com/ripple/filemanager/MainViewModel.kt'
with open(view_model_path, 'r', encoding='utf-8') as f:
    view_model = f.read()

if 'hasShizuku' not in view_model:
    view_model = re.sub(r'data class AppState\(', 'data class AppState(\n    val hasShizuku: Boolean = false,', view_model)
    view_model = view_model.replace('_state.update { it.copy(', '_state.update { it.copy(hasShizuku = repository.hasShizuku(), ')
    with open(view_model_path, 'w', encoding='utf-8') as f:
        f.write(view_model)

# Update SiftApp.kt
sift_app_path = 'app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt'
with open(sift_app_path, 'r', encoding='utf-8') as f:
    sift_app = f.read()

sift_app = re.sub(r'viewModel\.clearSelection\(\)', r'onAction(AppAction.ClearSelection)', sift_app)
sift_app = re.sub(r'TrashScreen\(viewModel = viewModel', r'TrashScreen(state = state, onAction = onAction', sift_app)
sift_app = re.sub(r'viewModel\.setLocation\(parent\)', r'onAction(AppAction.SetLocation(parent))', sift_app)
sift_app = re.sub(r'viewModel\.toggleViewMode\(\)', r'onAction(AppAction.ToggleViewMode)', sift_app)
sift_app = re.sub(r'viewModel\.setThemeMode\(([^)]+)\)', r'onAction(AppAction.SetThemeMode(\1))', sift_app)
sift_app = re.sub(r'viewModel\.setThemeHue\(([^)]+)\)', r'onAction(AppAction.SetThemeHue(\1))', sift_app)
sift_app = re.sub(r'viewModel\.setDynamicSystemTheme\(([^)]+)\)', r'onAction(AppAction.SetDynamicSystemTheme(\1))', sift_app)
sift_app = re.sub(r'viewModel\.setIconShape\(([^)]+)\)', r'onAction(AppAction.SetIconShape(\1))', sift_app)
sift_app = re.sub(r'viewModel\.setFontStyle\(([^)]+)\)', r'onAction(AppAction.SetFontStyle(\1))', sift_app)
sift_app = re.sub(r'viewModel\.toggleTextDecoration\(\)', r'onAction(AppAction.ToggleTextDecoration)', sift_app)
sift_app = re.sub(r'viewModel\.setMainTextScale\(([^)]+)\)', r'onAction(AppAction.SetMainTextScale(\1))', sift_app)
sift_app = re.sub(r'viewModel\.setSubTextScale\(([^)]+)\)', r'onAction(AppAction.SetSubTextScale(\1))', sift_app)
sift_app = re.sub(r'viewModel\.setCornerRoundness\(([^)]+)\)', r'onAction(AppAction.SetCornerRoundness(\1))', sift_app)
sift_app = re.sub(r'viewModel\.setQuery\(([^)]+)\)', r'onAction(AppAction.SetQuery(\1))', sift_app)
sift_app = re.sub(r'viewModel\.hasRestrictedAccess\(state\.location\)', r'(!state.location.contains("Android/data") && !state.location.contains("Android/obb") || state.hasShizuku)', sift_app)

with open(sift_app_path, 'w', encoding='utf-8') as f:
    f.write(sift_app)
