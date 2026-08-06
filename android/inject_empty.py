import os

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_empty = '''            } else if (files.isEmpty() && !state.isLoading) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.FolderOff, contentDescription = "Empty", modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No files found", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {'''

new_empty = '''            } else if (files.isEmpty() && !state.isLoading) {
                if (!viewModel.hasRestrictedAccess(state.currentLocation)) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Restricted", modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Access Restricted", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Android 11+ restricts access to this folder. Grant access using SAF or a privileged method.", textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.requestSafAccess(state.currentLocation) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Grant with SAF")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.requestRootAccess() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Grant with Root (libsu)")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.requestShizukuAccess() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Grant with Shizuku")
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.FolderOff, contentDescription = "Empty", modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No files found", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {'''

content = content.replace(old_empty, new_empty)

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(content)
