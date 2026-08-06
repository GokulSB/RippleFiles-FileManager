with open('SiftApp_head.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()
    
# Find BatchRenameDialog
start_idx = -1
for i, l in enumerate(lines):
    if 'fun BatchRenameDialog' in l:
        start_idx = i
        break

if start_idx != -1:
    print("Found BatchRenameDialog at", start_idx)
    # Print the rest of the functions
    funcs = []
    for i in range(start_idx, len(lines)):
        if lines[i].startswith('fun ') or lines[i].startswith('@Composable'):
            funcs.append(lines[i].strip())
    print("Functions after BatchRenameDialog:", funcs)
