import re

with open('../../../task.md', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('[/] Thumbnail for video files and duration display', '[x] Thumbnail for video files and duration display')

with open('../../../task.md', 'w', encoding='utf-8') as f:
    f.write(content)
