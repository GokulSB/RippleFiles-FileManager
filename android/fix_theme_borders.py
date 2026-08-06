import re

with open('app/src/main/java/com/ripple/filemanager/ui/ThemeBottomSheet.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix Icon Shape Box
old_icon_box = '''                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(shapeObj)
                                .background(bgColor)
                                .clickable { onIconShapeChange(shapeType) },'''
new_icon_box = '''                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(bgColor, shapeObj)
                                .border(1.dp, if (isActive) Color.Transparent else MaterialTheme.colorScheme.outline, shapeObj)
                                .clip(shapeObj)
                                .clickable { onIconShapeChange(shapeType) },'''
content = content.replace(old_icon_box, new_icon_box)

# Fix Font Style Surface
old_font_surface = '''                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = bgColor,
                            contentColor = contentColor,
                            onClick = { onFontStyleChange(style) }
                        ) {'''
new_font_surface = '''                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = bgColor,
                            contentColor = contentColor,
                            border = if (isActive) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            onClick = { onFontStyleChange(style) }
                        ) {'''
content = content.replace(old_font_surface, new_font_surface)

# Fix Text Decoration Surface
old_text_dec_surface = '''                        Surface(
                            shape = CircleShape,
                            color = bgColor,
                            contentColor = contentColor,
                            onClick = { onTextDecorationToggle(format) },
                            modifier = Modifier.size(42.dp)
                        ) {'''
new_text_dec_surface = '''                        Surface(
                            shape = CircleShape,
                            color = bgColor,
                            contentColor = contentColor,
                            border = if (isActive) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            onClick = { onTextDecorationToggle(format) },
                            modifier = Modifier.size(42.dp)
                        ) {'''
content = content.replace(old_text_dec_surface, new_text_dec_surface)

# Fix ModeButton Surface
old_mode_surface = '''    Surface(
        modifier = Modifier.height(42.dp),
        color = bgColor,
        contentColor = contentColor,
        shape = shape,
        onClick = onClick
    ) {'''
new_mode_surface = '''    Surface(
        modifier = Modifier.height(42.dp),
        color = bgColor,
        contentColor = contentColor,
        shape = shape,
        border = if (isActive) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick
    ) {'''
content = content.replace(old_mode_surface, new_mode_surface)

with open('app/src/main/java/com/ripple/filemanager/ui/ThemeBottomSheet.kt', 'w', encoding='utf-8') as f:
    f.write(content)
