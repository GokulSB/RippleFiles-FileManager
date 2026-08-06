import os
import re

with open('app/src/main/java/com/ripple/filemanager/ui/CleanerScreen.kt', 'r', encoding='utf-8') as f:
    cleaner_content = f.read()

cleaner_target = '''onClick = { },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF48FB1), contentColor = Color.Black)
                            ) {
                                Text("Manage storage")'''
cleaner_replacement = '''onClick = { 
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                                    try { context.startActivity(intent) } catch (e: Exception) {
                                        val fallback = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                                        try { context.startActivity(fallback) } catch (e2: Exception) {}
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF48FB1), contentColor = Color.Black)
                            ) {
                                Text("Manage storage")'''
cleaner_content = cleaner_content.replace(cleaner_target, cleaner_replacement)

# Ensure context is available
if 'val context = androidx.compose.ui.platform.LocalContext.current' not in cleaner_content:
    cleaner_content = cleaner_content.replace('fun CleanerScreen(viewModel: MainViewModel, state: com.ripple.filemanager.AppState) {', 'fun CleanerScreen(viewModel: MainViewModel, state: com.ripple.filemanager.AppState) {\n      val context = androidx.compose.ui.platform.LocalContext.current')

with open('app/src/main/java/com/ripple/filemanager/ui/CleanerScreen.kt', 'w', encoding='utf-8') as f:
    f.write(cleaner_content)
