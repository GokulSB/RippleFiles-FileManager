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

# 1. Title Area end - we want to remove the spacer before path indicator, but we can just leave it.
# Wait, the path indicator block:
path_start, path_end = find_block('Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)')
# The filter tabs block:
tabs_start, tabs_end = find_block('Row(\n                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),\n                    horizontalArrangement = Arrangement.spacedBy(8.dp)')
if tabs_start == -1: # Try another match just in case
    for i, line in enumerate(lines):
        if 'listOf(' in line and '"music" to Icons.Default.MusicNote' in lines[i+1]:
            # Backtrack to the row
            tabs_start = i - 4
            break
    if tabs_start != -1:
        open_braces = 0
        for i in range(tabs_start, len(lines)):
            open_braces += lines[i].count('{') - lines[i].count('}')
            if open_braces == 0:
                tabs_end = i + 1
                break

# The storage cards block:
storage_start, storage_end = find_block('if (state.storageTotalGb > 0f && !state.location.startsWith("drive")) {')

if path_start != -1 and tabs_start != -1 and storage_start != -1:
    path_block = lines[path_start:path_end]
    tabs_block = lines[tabs_start:tabs_end]
    storage_block = lines[storage_start:storage_end]
    
    # We want to remove path_block and tabs_block from their original positions (they are contiguous)
    # Then we replace the storage block with: storage_block + tabs_block + path_block
    
    # But wait, they might have spacers between them. Let's just do text replacement.
    new_lines = []
    i = 0
    while i < len(lines):
        if i == path_start:
            i = path_end
            continue
        if i == tabs_start:
            i = tabs_end
            continue
        if i == storage_start:
            new_lines.extend(storage_block)
            new_lines.extend(['\n                Spacer(modifier = Modifier.height(8.dp))\n'])
            new_lines.extend(tabs_block)
            new_lines.extend(['\n                Spacer(modifier = Modifier.height(8.dp))\n'])
            new_lines.extend(path_block)
            i = storage_end
            continue
        
        new_lines.append(lines[i])
        i += 1
        
    with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w') as f:
        f.writelines(new_lines)
    print("Successfully moved blocks")
else:
    print(f"Failed. path:{path_start}, tabs:{tabs_start}, storage:{storage_start}")

