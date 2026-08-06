import sys

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r') as f:
    lines = f.readlines()

# 1. Find Storage Cards block
start_storage = -1
end_storage = -1
for i, line in enumerate(lines):
    if 'if (state.storageTotalGb > 0f && !state.location.startsWith("drive")) {' in line:
        start_storage = i
        break

if start_storage != -1:
    open_braces = 0
    for i in range(start_storage, len(lines)):
        open_braces += lines[i].count('{') - lines[i].count('}')
        if open_braces == 0:
            # Check for Spacer right after it
            if i + 1 < len(lines) and 'Spacer(modifier = Modifier.height(8.dp))' in lines[i+1]:
                end_storage = i + 2
            else:
                end_storage = i + 1
            break

# 2. Find Path Indicator block
start_path = -1
end_path = -1
for i, line in enumerate(lines):
    if 'Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)' in line:
        start_path = i
        break

if start_path != -1:
    open_braces = 0
    for i in range(start_path, len(lines)):
        open_braces += lines[i].count('{') - lines[i].count('}')
        if open_braces == 0:
            end_path = i + 1
            break

if start_storage != -1 and start_path != -1:
    storage_block = lines[start_storage:end_storage]
    path_block = lines[start_path:end_path]

    # Swap them
    # Since storage is before path in the file:
    new_lines = lines[:start_storage] + path_block + lines[end_storage:start_path] + storage_block + lines[end_path:]
    
    with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w') as f:
        f.writelines(new_lines)
    print("Successfully swapped")
else:
    print(f"Could not find blocks. Storage: {start_storage}, Path: {start_path}")
