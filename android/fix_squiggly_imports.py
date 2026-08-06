import os

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

imports = '''import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.runtime.getValue
'''

if "import androidx.compose.animation.core.animateFloat" not in content:
    content = content.replace("import androidx.compose.foundation.layout.padding", "import androidx.compose.foundation.layout.padding\n" + imports)

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(content)
