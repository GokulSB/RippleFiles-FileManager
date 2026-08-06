with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = '''        DrawerMenuItem(
            icon = Icons.Outlined.Info,
            label = "About",
            cornerRoundness = cornerRoundness,
            onClick = {
                onCloseDrawer()
                onShowAbout()
            }
        )
    }
}
}

@Composable'''

replacement = r'''        DrawerMenuItem(
            icon = Icons.Outlined.Info,
            label = "About",
            cornerRoundness = cornerRoundness,
            onClick = {
                onCloseDrawer()
                onShowAbout()
            }
        )
        
        if (showGDrivePopup) {
            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE))
                .build()
            val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showGDrivePopup = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                title = { androidx.compose.material3.Text("Google Drive", style = MaterialTheme.typography.headlineSmall) },
                text = {
                    if (state.isGoogleDriveAuthenticated) {
                        androidx.compose.material3.Text("Logged in as:\n", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        androidx.compose.material3.Text("Sign in to Google Drive to access your cloud files securely.", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {
                    if (state.isGoogleDriveAuthenticated) {
                        androidx.compose.material3.TextButton(onClick = {
                            showGDrivePopup = false
                            onCloseDrawer()
                            onAction(AppAction.SetLocation("drive"))
                        }) {
                            androidx.compose.material3.Text("View Files", color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        androidx.compose.material3.TextButton(onClick = {
                            googleSignInClient.signOut().addOnCompleteListener {
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            }
                        }) {
                            androidx.compose.material3.Text("Login", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                dismissButton = {
                    if (state.isGoogleDriveAuthenticated) {
                        androidx.compose.material3.TextButton(onClick = {
                            googleSignInClient.signOut().addOnCompleteListener {
                                onAction(AppAction.SetGoogleDriveAuthStatus(false, null))
                            }
                            showGDrivePopup = false
                        }) {
                            androidx.compose.material3.Text("Logout", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        androidx.compose.material3.TextButton(onClick = { showGDrivePopup = false }) {
                            androidx.compose.material3.Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            )
        }
    }
}
}

@Composable'''

content = content.replace(target, replacement)

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print('Done!')
