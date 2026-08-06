import re

with open('app/src/main/java/com/ripple/filemanager/ui/FileGrid.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add annotations to FileListCard
content = re.sub(r'(?<!@Composable\n)fun FileListCard\(', '@OptIn(ExperimentalFoundationApi::class)\n@Composable\nfun FileListCard(', content)

# Replace file.name with buildHighlightedString(file.name, searchQuery) in FileListCard
content = content.replace('Text(file.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall',
                          'Text(buildHighlightedString(file.name, searchQuery), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall')

# Append buildHighlightedString if not exists
if 'fun buildHighlightedString' not in content:
    append_str = '''

fun buildHighlightedString(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return androidx.compose.ui.text.AnnotatedString(text)
    val startIndex = text.indexOf(query, ignoreCase = true)
    if (startIndex == -1) return androidx.compose.ui.text.AnnotatedString(text)
    val builder = androidx.compose.ui.text.AnnotatedString.Builder(text)
    builder.addStyle(androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color.Red), startIndex, startIndex + query.length)
    return builder.toAnnotatedString()
}
'''
    content += append_str

with open('app/src/main/java/com/ripple/filemanager/ui/FileGrid.kt', 'w', encoding='utf-8') as f:
    f.write(content)
