import re

with open('app/src/main/java/com/ripple/filemanager/ui/FileGrid.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# We want to replace the entire FileGridCard function
# Find the start of FileGridCard
start_match = re.search(r'@OptIn\(ExperimentalFoundationApi::class\)\n@Composable\nfun FileGridCard\(file: FileItem.*?\nfun FileListCard', content, re.DOTALL)
if start_match:
    print("Found FileGridCard")
    
    new_func = '''@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridCard(file: FileItem, isSelected: Boolean, iconShape: com.ripple.filemanager.IconShapeType, modifier: Modifier = Modifier, searchQuery: String = "", onClick: () -> Unit, onLongClick: () -> Unit, onPinClick: () -> Unit, onInfoClick: () -> Unit, onRenameClick: (String) -> Unit) {
    val shape = if (isSelected) RoundedCornerShape(topStart = 32.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 26.dp)
                else RoundedCornerShape(24.dp)
    
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val folderBgColor = if (isDarkTheme) Color(0xFF0A0704) else Color(0xFFFAF6F0)

    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.64f)
                  else if (file.type == "folder" && !file.isEmptyFolder) folderBgColor
                  else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    var showMenu by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var editNameValue by remember { mutableStateOf(file.name) }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    LaunchedEffect(isEditingName) {
        if (isEditingName) {
            focusRequester.requestFocus()
        }
    }

    val isMediaOrDoc = file.type == "image" || file.type == "video" || file.type == "doc" || listOf(".txt", ".json", ".md", ".csv", ".xml", ".log", ".kt", ".java", ".py", ".html").any { file.name.endsWith(it, ignoreCase = true) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 154.dp)
            .clip(shape)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = bgColor,
        shape = shape
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isMediaOrDoc) {
                if (file.type == "image" || file.type == "video") {
                    val imageLoader = remember(context) { coil.ImageLoader.Builder(context).components { add(coil.decode.VideoFrameDecoder.Factory()) }.build() }
                    coil.compose.AsyncImage(
                        model = java.io.File(file.path),
                        imageLoader = imageLoader,
                        contentDescription = file.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(bgColor), contentAlignment = Alignment.Center) {
                        FileShapeIcon(file.type, name = file.name, size = 64, path = file.path, iconShape = iconShape)
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 32.dp)
                ) {
                    Column {
                        if (isEditingName) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = editNameValue,
                                    onValueChange = { editNameValue = it },
                                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                                    textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, color = Color.White),
                                    singleLine = true,
                                    cursorBrush = SolidColor(Color.White)
                                )
                                IconButton(onClick = { 
                                    isEditingName = false
                                    if (editNameValue.isNotBlank() && editNameValue != file.name) onRenameClick(editNameValue)
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Check, contentDescription = "Confirm", modifier = Modifier.size(16.dp), tint = Color.White)
                                }
                            }
                        } else {
                            Text(buildHighlightedString(file.name, searchQuery), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, overflow = TextOverflow.Ellipsis, lineHeight = MaterialTheme.typography.titleSmall.lineHeight * 1.2, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(file.size, modifier = Modifier.weight(1f).padding(end = 8.dp), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.25f)) {
                                Text(file.changed, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                }
                
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(20.dp), tint = Color.White)
                    }
                    if (showMenu) {
                        val density = LocalDensity.current
                        val yOffset = with(density) { (-40).dp.roundToPx() }
                        Popup(alignment = Alignment.TopEnd, offset = androidx.compose.ui.unit.IntOffset(0, yOffset), onDismissRequest = { showMenu = false }, properties = androidx.compose.ui.window.PopupProperties(focusable = true)) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 4.dp) {
                                Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { showMenu = false; onPinClick() }, modifier = Modifier.size(36.dp)) { Icon(if (file.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin, contentDescription = "Pin") }
                                    IconButton(onClick = { 
                                        showMenu = false
                                        if (isNativeRootFolder(file.path)) { Toast.makeText(context, "Cannot rename native root folders", Toast.LENGTH_SHORT).show() } else { isEditingName = true; editNameValue = file.name }
                                    }, modifier = Modifier.size(36.dp)) { Icon(Icons.Outlined.Edit, contentDescription = "Rename") }
                                    IconButton(onClick = { showMenu = false; onInfoClick() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Outlined.Info, contentDescription = "Info") }
                                }
                            }
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.padding(14.dp).fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        FileShapeIcon(file.type, name = file.name, size = 48, path = file.path, iconShape = iconShape)
                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(17.dp))
                            }
                            if (showMenu) {
                                val density = LocalDensity.current
                                val yOffset = with(density) { (-52).dp.roundToPx() }
                                Popup(
                                    alignment = Alignment.TopEnd,
                                    offset = IntOffset(0, yOffset),
                                    onDismissRequest = { showMenu = false },
                                    properties = PopupProperties(focusable = true)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shadowElevation = 4.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { showMenu = false; onPinClick() }, modifier = Modifier.size(36.dp)) {
                                                Icon(if (file.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin, contentDescription = "Pin")
                                            }
                                            IconButton(onClick = { 
                                                showMenu = false
                                                if (isNativeRootFolder(file.path)) {
                                                    Toast.makeText(context, "Cannot rename native root folders", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    isEditingName = true
                                                    editNameValue = file.name
                                                }
                                            }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Outlined.Edit, contentDescription = "Rename")
                                            }
                                            IconButton(onClick = { showMenu = false; onInfoClick() }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Outlined.Info, contentDescription = "Info")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isEditingName) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = editNameValue,
                                onValueChange = { editNameValue = it },
                                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                                textStyle = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold, 
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                            )
                            IconButton(onClick = { 
                                isEditingName = false
                                if (editNameValue.isNotBlank() && editNameValue != file.name) {
                                    onRenameClick(editNameValue)
                                }
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Check, contentDescription = "Confirm", modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        Text(buildHighlightedString(file.name, searchQuery), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, overflow = TextOverflow.Ellipsis, lineHeight = MaterialTheme.typography.titleSmall.lineHeight * 1.2, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(file.size, modifier = Modifier.weight(1f).padding(end = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(file.changed, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            if (file.isPinned) {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 2.dp).size(28.dp).graphicsLayer(rotationZ = 45f)
                )
            }
        }
    }
}

fun FileListCard'''

    # Ensure maxLines is removed for FileListCard as well just in case git restore reverted it
    # Wait, I already removed maxLines using powershell above before running this python script, 
    # but the regex \nfun FileListCard means I'll replace everything up to un FileListCard
    # and then put it back.
    
    new_content = content[:start_match.start()] + new_func + content[start_match.end()-16:]
    with open('app/src/main/java/com/ripple/filemanager/ui/FileGrid.kt', 'w', encoding='utf-8') as f:
        f.write(new_content)
    print("Replaced FileGridCard")
else:
    print("Could not find FileGridCard")
