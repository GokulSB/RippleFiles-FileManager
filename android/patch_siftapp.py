import sys
import re

with open("app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt", "r") as f:
    content = f.read()

# Replace the TopBar Search and Menu icons
# Current code is from `AnimatedVisibility(visible = isSearchExpanded` to the end of `Box { ... DropdownMenu`

pattern_topbar = r'''(                    androidx\.compose\.animation\.AnimatedVisibility\(\s*visible = isSearchExpanded.*?singleLine = true\s*\)\s*\}\s*if \(!isSearchExpanded\) \{\s*Spacer\(modifier = Modifier\.width\(8\.dp\)\)\s*Box\(\s*modifier = Modifier\s*\.clip\(getDynamicCornerShape\(12f, state\.cornerRoundness\)\)\s*\.background\(MaterialTheme\.colorScheme\.surfaceContainerHigh\)\s*\.clickable \{ isSearchExpanded = true \}\s*\.padding\(12\.dp\),\s*contentAlignment = Alignment\.Center\s*\) \{\s*Icon\(Icons\.Default\.Search, contentDescription = "Search", modifier = Modifier\.size\(24\.dp\)\)\s*\}\s*\}\s*Box \{\s*var showTopMenu by remember \{ mutableStateOf\(false\) \}\s*Box\(\s*modifier = Modifier\s*\.clip\(getDynamicCornerShape\(12f, state\.cornerRoundness\)\)\s*\.background\(MaterialTheme\.colorScheme\.surfaceContainerHigh\)\s*\.clickable \{ showTopMenu = true \}\s*\.padding\(12\.dp\),\s*contentAlignment = Alignment\.Center\s*\) \{\s*Icon\(Icons\.Default\.MoreVert, contentDescription = "Menu", modifier = Modifier\.size\(24\.dp\)\)\s*\})'''

replacement_topbar = '''                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(getDynamicCornerShape(12f, state.cornerRoundness))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Box(
                                modifier = Modifier
                                    .clickable { isSearchExpanded = true }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(24.dp))
                            }
                            
                            androidx.compose.material3.DropdownMenu(
                                expanded = isSearchExpanded,
                                onDismissRequest = { isSearchExpanded = false },
                                modifier = Modifier.padding(8.dp).width(250.dp),
                                shape = getDynamicCornerShape(16f, state.cornerRoundness)
                            ) {
                                TextField(
                                    value = state.query,
                                    onValueChange = { onAction(AppAction.SetQuery(it)) },
                                    placeholder = { Text("Search", maxLines = 1, softWrap = false) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    trailingIcon = { 
                                        IconButton(onClick = { 
                                            isSearchExpanded = false
                                            onAction(AppAction.SetQuery(""))
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = getDynamicCornerShape(24f, state.cornerRoundness),
                                    colors = TextFieldDefaults.colors(
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    ),
                                    singleLine = true
                                )
                            }
                        }
                        
                        Box {
                            var showTopMenu by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .clickable { showTopMenu = true }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu", modifier = Modifier.size(24.dp))
                            }'''

if re.search(pattern_topbar, content, flags=re.DOTALL):
    content = re.sub(pattern_topbar, replacement_topbar, content, count=1, flags=re.DOTALL)
    print("Topbar replaced.")
else:
    print("Failed to replace TopBar")

# StorageCards layout
pattern_storage_cards_parent = r'''(                    Row\(\s*modifier = Modifier\s*\.fillMaxWidth\(\)\s*\.horizontalScroll\(rememberScrollState\(\)\),\s*horizontalArrangement = Arrangement\.spacedBy\(12\.dp\)\s*\) \{)'''
replacement_storage_cards_parent = '''                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {'''

if re.search(pattern_storage_cards_parent, content):
    content = re.sub(pattern_storage_cards_parent, replacement_storage_cards_parent, content, count=1)
    print("Storage parent replaced.")
else:
    print("Failed to replace StorageCards parent")

# StorageCard calls
pattern_storage_card_1 = r'''(                        StorageCard\(\s*icon = Icons\.Default\.SdStorage,)'''
replacement_storage_card_1 = '''                        StorageCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.SdStorage,'''

if re.search(pattern_storage_card_1, content):
    content = re.sub(pattern_storage_card_1, replacement_storage_card_1, content, count=1)
    print("Storage card 1 replaced.")

pattern_storage_card_2 = r'''(                            StorageCard\(\s*icon = Icons\.Default\.Cloud,)'''
replacement_storage_card_2 = '''                            StorageCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Cloud,'''
if re.search(pattern_storage_card_2, content):
    content = re.sub(pattern_storage_card_2, replacement_storage_card_2, content, count=1)
    print("Storage card 2 replaced.")

# StorageCard function definition
pattern_storage_card_def = r'''(fun StorageCard\(\s*icon: androidx\.compose\.ui\.graphics\.vector\.ImageVector,\s*storageText: String,\s*progress: Float,\s*cornerRoundness: Float,\s*onClick: \(\) -> Unit\s*\) \{\s*Surface\(\s*modifier = Modifier\s*\.clip\(getDynamicCornerShape\(16f, cornerRoundness\)\)\s*\.clickable\(onClick = onClick\),\s*color = MaterialTheme\.colorScheme\.surfaceContainerHigh,\s*shape = getDynamicCornerShape\(16f, cornerRoundness\)\s*\) \{\s*Row\(\s*modifier = Modifier\.padding\(horizontal = 12\.dp, vertical = 8\.dp\),\s*verticalAlignment = Alignment\.CenterVertically\s*\) \{\s*Box\(contentAlignment = Alignment\.Center\) \{\s*androidx\.compose\.material3\.CircularProgressIndicator\(\s*progress = \{ progress \},\s*modifier = Modifier\.size\(28\.dp\),\s*color = MaterialTheme\.colorScheme\.primary,\s*trackColor = MaterialTheme\.colorScheme\.onSurface\.copy\(alpha = 0\.1f\),\s*strokeWidth = 4\.dp,\s*strokeCap = androidx\.compose\.ui\.graphics\.StrokeCap\.Round\s*\)\s*Icon\(\s*imageVector = icon,\s*contentDescription = null,\s*tint = MaterialTheme\.colorScheme\.onSurfaceVariant,\s*modifier = Modifier\.size\(16\.dp\)\s*\)\s*\}\s*Spacer\(modifier = Modifier\.width\(12\.dp\)\)\s*Text\(\s*text = storageText,\s*style = MaterialTheme\.typography\.labelLarge,\s*fontWeight = FontWeight\.Bold,\s*color = MaterialTheme\.colorScheme\.onSurface\s*\)\s*\}\s*\}\s*\})'''

replacement_storage_card_def = '''fun StorageCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    storageText: String,
    progress: Float,
    cornerRoundness: Float,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(getDynamicCornerShape(16f, cornerRoundness))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = getDynamicCornerShape(16f, cornerRoundness)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    strokeWidth = 4.dp,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = storageText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}'''

if re.search(pattern_storage_card_def, content):
    content = re.sub(pattern_storage_card_def, replacement_storage_card_def, content, count=1)
    print("StorageCard def replaced.")
else:
    print("Failed to replace StorageCard def")

with open("app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt", "w") as f:
    f.write(content)
