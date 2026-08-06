import re

with open('app/src/main/java/com/ripple/filemanager/MainActivity.kt', 'r', encoding='utf-8') as f:
    c = f.read()

# Fix LoadFileDetails with coroutine
c = c.replace('is AppAction.LoadFileDetails -> {\n                            val details = viewModel.getFileDetails(action.path)\n                            action.onLoaded(details)\n                        }',
              'is AppAction.LoadFileDetails -> {\n                            androidx.lifecycle.lifecycleScope.launch {\n                                val details = viewModel.getFileDetails(action.path)\n                                action.onLoaded(details)\n                            }\n                        }')

# Fix refreshTrash
c = c.replace('viewModel.refreshTrash()', 'viewModel.loadTrashFiles()')

with open('app/src/main/java/com/ripple/filemanager/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(c)

# Fix AppAction.kt for IconShapeType
with open('app/src/main/java/com/ripple/filemanager/AppAction.kt', 'r', encoding='utf-8') as f:
    c = f.read()

c = c.replace('data class SetIconShape(val shape: String)', 'data class SetIconShape(val shape: com.ripple.filemanager.IconShapeType)')

with open('app/src/main/java/com/ripple/filemanager/AppAction.kt', 'w', encoding='utf-8') as f:
    f.write(c)
