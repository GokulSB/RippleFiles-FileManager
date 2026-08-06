import re
with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()
indent = 0
started = False
for i, line in enumerate(lines):
    if i < 554: continue
    
    line_clean = re.sub(r'//.*', '', line)
    line_clean = re.sub(r'".*?"', '""', line_clean)
    
    for char in line_clean:
        if char == '{': 
            indent += 1
            started = True
        elif char == '}': 
            indent -= 1
            if started and indent == 0:
                print(f'Scaffold lambda closes at line {i+1}')
