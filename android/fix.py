import sys

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r') as f:
    lines = f.readlines()

def find_block(start_str):
    start = -1
    for i, line in enumerate(lines):
        if start_str in line:
            start = i
            break
    if start == -1:
        return -1, -1
    open_braces = 0
    end = -1
    for i in range(start, len(lines)):
        open_braces += lines[i].count('{') - lines[i].count('}')
        if open_braces == 0:
            if i + 1 < len(lines) and 'Spacer(modifier = Modifier.height(8.dp))' in lines[i+1]:
                end = i + 2
            else:
                end = i + 1
            break
    return start, end

tabs_start = -1
for i, line in enumerate(lines):
    if 'listOf(' in line and '"music" to Icons.Default.MusicNote' in lines[i+1]:
        tabs_start = i - 3
        break

tabs_end = -1
if tabs_start != -1:
    open_braces = 0
    for i in range(tabs_start, len(lines)):
        open_braces += lines[i].count('{') - lines[i].count('}')
        if open_braces == 0:
            tabs_end = i + 1
            break

path_start, path_end = find_block('Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)')
storage_start, storage_end = find_block('if (state.storageTotalGb > 0f && !state.location.startsWith("drive")) {')

if tabs_start != -1 and path_start != -1 and storage_start != -1:
    tabs_block = lines[tabs_start:tabs_end]
    path_block = lines[path_start:path_end]
    storage_block = lines[storage_start:storage_end]
    
    # We will remove tabs from topBar, and replace the whole header contents
    new_lines = []
    i = 0
    while i < len(lines):
        # Skip the original tabs block entirely
        if i == tabs_start:
            i = tabs_end
            continue
            
        # We also need to skip the original path block inside header
        if i == path_start:
            i = path_end
            continue
            
        # At storage block, we rewrite the whole header content!
        if i == storage_start:
            new_lines.append("                Column {\n")
            new_lines.extend(storage_block)
            new_lines.append("                    Spacer(modifier = Modifier.height(8.dp))\n")
            new_lines.extend(tabs_block)
            new_lines.append("                    Spacer(modifier = Modifier.height(8.dp))\n")
            new_lines.extend(path_block)
            new_lines.append("                }\n")
            i = storage_end
            # If there's a Spacer right after storage block, skip it manually or it will just be extra space, that's fine
            continue
            
        new_lines.append(lines[i])
        i += 1
        
    with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w') as f:
        f.writelines(new_lines)
    print("Successfully reorganized header with Column")
else:
    print(f"Failed. tabs:{tabs_start}, path:{path_start}, storage:{storage_start}")
