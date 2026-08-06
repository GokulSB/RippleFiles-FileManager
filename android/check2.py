with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()
indent = 0
for i, line in enumerate(lines):
    in_str = False
    escape = False
    for char in line:
        if escape: 
            escape = False
            continue
        if char == '\\': 
            escape = True
            continue
        if char == '"': 
            in_str = not in_str
            continue
        if in_str: 
            continue
        if char == '{': 
            indent += 1
        elif char == '}': 
            indent -= 1
    if indent == 0 and i >= 76:
        print(f'SiftApp ends at line {i+1}')
        break
