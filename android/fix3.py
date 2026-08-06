import re

with open('app/src/main/java/com/ripple/filemanager/ui/FileGrid.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix FileGrid signature
old_sig = '''fun FileGrid(
    files: List<FileItem>,
    selectedFiles: Set<Int>,
    isListMode: Boolean,
    iconShape: com.ripple.filemanager.IconShapeType,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit,
    onPinClick: (FileItem) -> Unit,
    onInfoClick: (FileItem) -> Unit,
    onRenameClick: (FileItem, String) -> Unit,
    modifier: Modifier = Modifier
)'''
new_sig = '''fun FileGrid(
    files: List<FileItem>,
    selectedFiles: Set<Int>,
    isListMode: Boolean,
    iconShape: com.ripple.filemanager.IconShapeType,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit,
    onPinClick: (FileItem) -> Unit,
    onInfoClick: (FileItem) -> Unit,
    onRenameClick: (FileItem, String) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = ""
)'''
content = content.replace(old_sig, new_sig)

# Fix FileListCard invocation
old_list = '''                FileListCard(
                    file = file,
                    isSelected = selectedFiles.contains(file.id),
                    iconShape = iconShape,'''
new_list = '''                FileListCard(
                    file = file,
                    isSelected = selectedFiles.contains(file.id),
                    iconShape = iconShape,
                    searchQuery = searchQuery,'''
content = content.replace(old_list, new_list)

# Fix FileGridCard invocation
old_grid = '''                FileGridCard(
                    file = file,
                    isSelected = selectedFiles.contains(file.id),
                    iconShape = iconShape,'''
new_grid = '''                FileGridCard(
                    file = file,
                    isSelected = selectedFiles.contains(file.id),
                    iconShape = iconShape,
                    searchQuery = searchQuery,'''
content = content.replace(old_grid, new_grid)

with open('app/src/main/java/com/ripple/filemanager/ui/FileGrid.kt', 'w', encoding='utf-8') as f:
    f.write(content)
