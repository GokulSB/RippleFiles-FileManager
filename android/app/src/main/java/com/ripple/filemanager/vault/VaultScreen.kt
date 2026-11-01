package com.ripple.filemanager.vault

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.AppState
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.haptics.LocalHaptics
import com.ripple.filemanager.ui.FilePickerDialog
import com.ripple.filemanager.ui.appGradientBackground
import com.ripple.filemanager.ui.expressive.*
import com.ripple.filemanager.ui.theme.LocalAppFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun VaultScreen(
    state: AppState,
    onAction: (AppAction) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHaptics.current

    var isVaultSetup by remember { mutableStateOf(VaultManager.isVaultSetup(context)) }
    var isVaultUnlocked by rememberSaveable { mutableStateOf(false) }

    var isImporting by remember { mutableStateOf(false) }
    var showFilePicker by remember { mutableStateOf(false) }
    var vaultRefreshTrigger by remember { mutableIntStateOf(0) }

    // Top-level unconditionally registered ActivityResult launcher for safe fallback system file picking
    val systemPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data!!
            val uris = mutableListOf<Uri>()
            data.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) {
                    clip.getItemAt(i)?.uri?.let { uris.add(it) }
                }
            }
            if (uris.isEmpty()) {
                data.data?.let { uris.add(it) }
            }
            if (uris.isNotEmpty()) {
                isImporting = true
                coroutineScope.launch(Dispatchers.IO) {
                    val count = VaultManager.importFiles(context, uris)
                    withContext(Dispatchers.Main) {
                        isImporting = false
                        vaultRefreshTrigger++
                        haptics.tap()
                        Toast.makeText(context, "Added $count file(s) to Vault", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val launchSafeSystemPicker: () -> Unit = {
        try {
            val openDoc = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            systemPickerLauncher.launch(openDoc)
        } catch (e: Exception) {
            try {
                val getContent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                systemPickerLauncher.launch(Intent.createChooser(getContent, "Select Files"))
            } catch (ex: Exception) {
                Toast.makeText(context, "No file picker available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler {
        if (isVaultUnlocked) {
            isVaultUnlocked = false
        }
        onClose()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .appGradientBackground()
    ) {
        when {
            !isVaultSetup -> {
                VaultSetupWizard(
                    onSetupCompleted = {
                        isVaultSetup = true
                        isVaultUnlocked = true
                    },
                    onClose = onClose
                )
            }
            !isVaultUnlocked -> {
                VaultUnlockScreen(
                    onUnlocked = { isVaultUnlocked = true },
                    onClose = onClose
                )
            }
            else -> {
                VaultBrowserScreen(
                    onLock = { isVaultUnlocked = false },
                    onClose = {
                        isVaultUnlocked = false
                        onClose()
                    },
                    onAction = onAction,
                    isImporting = isImporting,
                    onAddFilesClick = { showFilePicker = true },
                    refreshTrigger = vaultRefreshTrigger
                )
            }
        }

        if (showFilePicker) {
            FilePickerDialog(
                title = "Add Files to Vault",
                confirmText = "Add to Vault",
                cornerRoundness = state.cornerRoundness,
                onDismiss = { showFilePicker = false },
                onFilesSelected = { selectedFiles ->
                    showFilePicker = false
                    if (selectedFiles.isNotEmpty()) {
                        isImporting = true
                        coroutineScope.launch(Dispatchers.IO) {
                            val count = VaultManager.importFilesFromPaths(context, selectedFiles.map { it.path })
                            withContext(Dispatchers.Main) {
                                isImporting = false
                                vaultRefreshTrigger++
                                haptics.tap()
                                Toast.makeText(context, "Added $count file(s) to Vault", Toast.LENGTH_SHORT).show()
                                onAction(AppAction.Reload)
                            }
                        }
                    }
                },
                onOpenSystemPicker = {
                    launchSafeSystemPicker()
                }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// STEPPER HEADER
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun StepperIndicator(currentStep: Int, totalSteps: Int = 3) {
    val colors = ExpressiveTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..totalSteps) {
            val isActive = i <= currentStep
            Box(
                modifier = Modifier
                    .width(if (i == currentStep) 24.dp else 8.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isActive) colors.accent else colors.line.copy(alpha = colors.lineAlpha))
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// VAULT SETUP WIZARD (3 PAGES)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun VaultSetupWizard(
    onSetupCompleted: () -> Unit,
    onClose: () -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var hint by rememberSaveable { mutableStateOf("") }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            if (targetState > initialState) {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            } else {
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            }
        },
        label = "vault_setup_step"
    ) { currentStep ->
        when (currentStep) {
            1 -> {
                VaultSetupStepOne(
                    onContinue = { step = 2 },
                    onClose = onClose
                )
            }
            2 -> {
                VaultSetupStepTwo(
                    initialPassword = password,
                    initialConfirm = confirmPassword,
                    initialHint = hint,
                    onBack = { step = 1 },
                    onContinue = { pwd, conf, hnt ->
                        password = pwd
                        confirmPassword = conf
                        hint = hnt
                        step = 3
                    }
                )
            }
            3 -> {
                VaultSetupStepThree(
                    password = password,
                    hint = hint,
                    onBack = { step = 2 },
                    onFinish = {
                        onSetupCompleted()
                    }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// PAGE 1: Welcome & Offline / Do Not Forget Notice
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun VaultSetupStepOne(
    onContinue: () -> Unit,
    onClose: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CookieIconButton(
                onClick = onClose,
                icon = Icons.Default.Close,
                bgColor = colors.card,
                iconTint = colors.text,
                description = "Cancel setup",
                size = 40.dp,
                lobes = 8
            )
            StepperIndicator(currentStep = 1, totalSteps = 3)
            Spacer(modifier = Modifier.width(40.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hero cookie icon
        CookieIcon(
            icon = Icons.Outlined.Security,
            bgColor = colors.card,
            iconTint = colors.accent,
            size = 80.dp,
            lobes = 10
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Welcome to Vault",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LocalAppFont.current,
            color = colors.text,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Zero-knowledge offline file protection",
            fontSize = 14.sp,
            fontFamily = LocalAppFont.current,
            color = colors.muted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Notice Cards
        SetupInfoCard(
            icon = Icons.Outlined.CloudOff,
            iconTint = colors.accent,
            title = "100% Offline & On-Device",
            description = "Ripple Vault operates strictly on your phone. Your files and encryption keys never leave this device. No servers, no tracking, and no internet access required."
        )

        Spacer(modifier = Modifier.height(12.dp))

        SetupInfoCard(
            icon = Icons.Outlined.KeyOff,
            iconTint = colors.accent,
            title = "No Cloud Recovery Available",
            description = "Because there are zero cloud accounts or remote servers, there is NO 'Forgot Password' email or reset button. Nobody in the world can reset it for you."
        )

        Spacer(modifier = Modifier.height(12.dp))

        SetupInfoCard(
            icon = Icons.Outlined.WarningAmber,
            iconTint = Color(0xFFE6A700),
            title = "Important: Do Not Forget Your Password",
            description = "If you lose your password, your vaulted files cannot be recovered. In the next steps, you will create a password and save a backup text file for safe recovery."
        )

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(32.dp))

        // Next button
        Button(
            onClick = {
                haptics.tap()
                onContinue()
            },
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.ink
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                text = "Set Up Vault Password",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = LocalAppFont.current
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// PAGE 2: Set Password, Retype Password & Hint
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun VaultSetupStepTwo(
    initialPassword: String,
    initialConfirm: String,
    initialHint: String,
    onBack: () -> Unit,
    onContinue: (password: String, confirm: String, hint: String) -> Unit
) {
    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current

    var password by rememberSaveable { mutableStateOf(initialPassword) }
    var confirmPassword by rememberSaveable { mutableStateOf(initialConfirm) }
    var hint by rememberSaveable { mutableStateOf(initialHint) }

    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible by rememberSaveable { mutableStateOf(false) }

    val isMinLength = password.length >= 4
    val isMatching = password.isNotEmpty() && password == confirmPassword
    val isValid = isMinLength && isMatching

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CookieIconButton(
                onClick = onBack,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                bgColor = colors.card,
                iconTint = colors.text,
                description = "Previous step",
                size = 40.dp,
                lobes = 8
            )
            StepperIndicator(currentStep = 2, totalSteps = 3)
            Spacer(modifier = Modifier.width(40.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        CookieIcon(
            icon = Icons.Outlined.Lock,
            bgColor = colors.card,
            iconTint = colors.accent,
            size = 64.dp,
            lobes = 8
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Create Vault Password",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LocalAppFont.current,
            color = colors.text,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Choose a master password and hint. You will need this to unlock your vault.",
            fontSize = 13.sp,
            fontFamily = LocalAppFont.current,
            color = colors.muted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Input 1: Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Vault Password", fontFamily = LocalAppFont.current) },
            placeholder = { Text("At least 4 characters", fontFamily = LocalAppFont.current) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.card,
                unfocusedContainerColor = colors.card,
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.line.copy(alpha = colors.lineAlpha),
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text
            ),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = colors.muted
                )
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = colors.muted
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Input 2: Retype Password
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Retype Password", fontFamily = LocalAppFont.current) },
            placeholder = { Text("Re-enter master password", fontFamily = LocalAppFont.current) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.card,
                unfocusedContainerColor = colors.card,
                focusedBorderColor = if (confirmPassword.isNotEmpty() && !isMatching) MaterialTheme.colorScheme.error else colors.accent,
                unfocusedBorderColor = if (confirmPassword.isNotEmpty() && !isMatching) MaterialTheme.colorScheme.error else colors.line.copy(alpha = colors.lineAlpha),
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text
            ),
            visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.LockClock,
                    contentDescription = null,
                    tint = colors.muted
                )
            },
            trailingIcon = {
                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                    Icon(
                        imageVector = if (confirmVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (confirmVisible) "Hide password" else "Show password",
                        tint = colors.muted
                    )
                }
            }
        )

        if (confirmPassword.isNotEmpty() && !isMatching) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Passwords do not match",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontFamily = LocalAppFont.current,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
        } else if (password.isNotEmpty() && !isMinLength) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Password must be at least 4 characters",
                color = colors.muted,
                fontSize = 12.sp,
                fontFamily = LocalAppFont.current,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Input 3: Password Hint
        OutlinedTextField(
            value = hint,
            onValueChange = { hint = it },
            label = { Text("Password Hint (Recommended)", fontFamily = LocalAppFont.current) },
            placeholder = { Text("e.g. My first car + hometown", fontFamily = LocalAppFont.current) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.card,
                unfocusedContainerColor = colors.card,
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.line.copy(alpha = colors.lineAlpha),
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = colors.muted
                )
            }
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "This hint will appear on the unlock screen if you ever need a clue.",
            color = colors.muted,
            fontSize = 12.sp,
            fontFamily = LocalAppFont.current,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(32.dp))

        // Continue button
        Button(
            onClick = {
                if (isValid) {
                    haptics.tap()
                    onContinue(password, confirmPassword, hint)
                }
            },
            enabled = isValid,
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.ink,
                disabledContainerColor = colors.accent.copy(alpha = 0.35f),
                disabledContentColor = colors.ink.copy(alpha = 0.35f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                text = "Continue to Backup",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = LocalAppFont.current
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// PAGE 3: Save Password as Text File & Recovery Warning
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun VaultSetupStepThree(
    password: String,
    hint: String,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current
    val coroutineScope = rememberCoroutineScope()

    var isSavedToFile by rememberSaveable { mutableStateOf(false) }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isAcknowledged by rememberSaveable { mutableStateOf(false) }

    val backupContent = remember(password, hint) {
        VaultManager.generateBackupText(password, hint)
    }

    // SAF Create Document launcher for saving as text file
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(backupContent.toByteArray(Charsets.UTF_8))
                }
                isSavedToFile = true
                haptics.tap()
                Toast.makeText(context, "Password backup saved successfully!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CookieIconButton(
                onClick = onBack,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                bgColor = colors.card,
                iconTint = colors.text,
                description = "Previous step",
                size = 40.dp,
                lobes = 8
            )
            StepperIndicator(currentStep = 3, totalSteps = 3)
            Spacer(modifier = Modifier.width(40.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Warning Cookie Icon
        CookieIcon(
            icon = Icons.Outlined.WarningAmber,
            bgColor = colors.card,
            iconTint = Color(0xFFE6A700),
            size = 64.dp,
            lobes = 8
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Save Password Backup",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LocalAppFont.current,
            color = colors.text,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Critical recovery step for offline security",
            fontSize = 13.sp,
            fontFamily = LocalAppFont.current,
            color = colors.muted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Alert Warning Box
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE6A700).copy(alpha = 0.12f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE6A700).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFE6A700),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Recovery Warning",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = LocalAppFont.current,
                        color = colors.text
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Remember: Ripple Files cannot recover your vault if you lose this password. Save this text file now and store it in a secure location (such as external storage, another device, or safe notes).",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontFamily = LocalAppFont.current,
                        color = colors.text
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Password Preview Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.line.copy(alpha = colors.lineAlpha)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VAULT PASSWORD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = LocalAppFont.current,
                            color = colors.muted,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isPasswordVisible) password else "•".repeat(password.length.coerceAtLeast(8)),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = LocalAppFont.current,
                            color = colors.text
                        )
                    }
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (isPasswordVisible) "Hide" else "Show",
                            tint = colors.muted
                        )
                    }
                }

                if (hint.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.line.copy(alpha = colors.lineAlpha * 0.5f))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "PASSWORD HINT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = LocalAppFont.current,
                        color = colors.muted,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = hint,
                        fontSize = 14.sp,
                        fontFamily = LocalAppFont.current,
                        color = colors.text
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Save as Text File Button
        OutlinedButton(
            onClick = {
                haptics.tap()
                createDocumentLauncher.launch("ripple_vault_password_backup.txt")
            },
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isSavedToFile) colors.accent else colors.line.copy(alpha = colors.lineAlpha)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isSavedToFile) colors.accent.copy(alpha = 0.12f) else colors.card
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(
                imageVector = if (isSavedToFile) Icons.Outlined.CheckCircle else Icons.Outlined.SaveAlt,
                contentDescription = null,
                tint = if (isSavedToFile) colors.accent else colors.text,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isSavedToFile) "Password Backup Saved ✓" else "Save Password as Text File",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = LocalAppFont.current,
                color = if (isSavedToFile) colors.accent else colors.text
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Share / Copy Backup
        OutlinedButton(
            onClick = {
                haptics.tap()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Ripple Vault Recovery", backupContent)
                clipboard.setPrimaryClip(clip)

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Ripple Vault Password Backup")
                    putExtra(Intent.EXTRA_TEXT, backupContent)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share or Save Password"))
            },
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.line.copy(alpha = colors.lineAlpha)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.card),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Share or Copy to Safe Notes",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = LocalAppFont.current,
                color = colors.muted
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Acknowledgment Checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { isAcknowledged = !isAcknowledged }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isAcknowledged,
                onCheckedChange = { isAcknowledged = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = colors.accent,
                    checkmarkColor = colors.ink
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "I understand that without this password, my vaulted files cannot be recovered.",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontFamily = LocalAppFont.current,
                color = colors.text
            )
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(28.dp))

        // Finish setup button
        Button(
            onClick = {
                if (isAcknowledged) {
                    haptics.tap()
                    VaultManager.setupVault(context, password, hint)
                    Toast.makeText(context, "Vault setup completed successfully!", Toast.LENGTH_SHORT).show()
                    onFinish()
                }
            },
            enabled = isAcknowledged,
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.ink,
                disabledContainerColor = colors.accent.copy(alpha = 0.35f),
                disabledContentColor = colors.ink.copy(alpha = 0.35f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                text = "Finish Setup & Open Vault",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = LocalAppFont.current
            )
        }
    }
}

@Composable
private fun SetupInfoCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    val colors = ExpressiveTheme.colors
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.line.copy(alpha = colors.lineAlpha)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            CookieIcon(
                icon = icon,
                bgColor = iconTint.copy(alpha = 0.15f),
                iconTint = iconTint,
                size = 40.dp,
                lobes = 8
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = LocalAppFont.current,
                    color = colors.text
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = LocalAppFont.current,
                    color = colors.muted
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// UNLOCK SCREEN (WHEN VAULT IS ALREADY SETUP)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun VaultUnlockScreen(
    onUnlocked: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current

    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showHint by rememberSaveable { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("sift_prefs", Context.MODE_PRIVATE) }
    val biometricEnabled = remember { prefs.getBoolean("lock_biometric_enabled", false) }
    val hint = remember { VaultManager.getVaultPasswordHint(context) }

    fun triggerBiometrics() {
        if (biometricEnabled && context is androidx.fragment.app.FragmentActivity) {
            val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
            val callback = object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    haptics.tap()
                    onUnlocked()
                }
            }
            val biometricPrompt = androidx.biometric.BiometricPrompt(context, executor, callback)
            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Vault")
                .setSubtitle("Use your device credential to verify access")
                .setAllowedAuthenticators(
                    androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
            biometricPrompt.authenticate(promptInfo)
        }
    }

    LaunchedEffect(Unit) {
        if (biometricEnabled) {
            triggerBiometrics()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            CookieIconButton(
                onClick = onClose,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                bgColor = colors.card,
                iconTint = colors.text,
                description = "Back",
                size = 40.dp,
                lobes = 8
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        CookieIcon(
            icon = Icons.Outlined.Lock,
            bgColor = colors.card,
            iconTint = colors.accent,
            size = 72.dp,
            lobes = 8
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Unlock Vault",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LocalAppFont.current,
            color = colors.text,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Enter your vault password to access your encrypted files",
            fontSize = 13.sp,
            fontFamily = LocalAppFont.current,
            color = colors.muted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Vault Password", fontFamily = LocalAppFont.current) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.card,
                unfocusedContainerColor = colors.card,
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.line.copy(alpha = colors.lineAlpha),
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text
            ),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = colors.muted
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (password.isNotEmpty()) {
                    if (VaultManager.verifyPassword(context, password)) {
                        haptics.tap()
                        onUnlocked()
                    } else {
                        errorMessage = "Incorrect password. Try again or check your hint."
                    }
                }
            })
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontFamily = LocalAppFont.current,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hint button / expansion
        if (hint.isNotBlank()) {
            TextButton(onClick = { showHint = !showHint }) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showHint) "Hide Password Hint" else "Need Help? View Hint",
                    color = colors.accent,
                    fontSize = 13.sp,
                    fontFamily = LocalAppFont.current
                )
            }

            AnimatedVisibility(visible = showHint) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.line.copy(alpha = colors.lineAlpha)),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "HINT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.muted,
                            letterSpacing = 0.8.sp,
                            fontFamily = LocalAppFont.current
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = hint,
                            fontSize = 14.sp,
                            color = colors.text,
                            fontFamily = LocalAppFont.current
                        )
                    }
                }
            }
        }

        if (biometricEnabled && context is androidx.fragment.app.FragmentActivity) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { triggerBiometrics() }) {
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Use Fingerprint",
                    color = colors.accent,
                    fontSize = 13.sp,
                    fontFamily = LocalAppFont.current
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(32.dp))

        // Unlock button
        Button(
            onClick = {
                if (password.isNotEmpty()) {
                    if (VaultManager.verifyPassword(context, password)) {
                        haptics.tap()
                        onUnlocked()
                    } else {
                        errorMessage = "Incorrect password. Try again or check your hint."
                    }
                }
            },
            enabled = password.isNotEmpty(),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.ink,
                disabledContainerColor = colors.accent.copy(alpha = 0.35f),
                disabledContentColor = colors.ink.copy(alpha = 0.35f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                text = "Unlock Vault",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = LocalAppFont.current
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// VAULT BROWSER SCREEN (UNLOCKED STATE)
// ══════════════════════════════════════════════════════════════════════════════
private data class VaultCategoryItem(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val pastelKey: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultBrowserScreen(
    onLock: () -> Unit,
    onClose: () -> Unit,
    onAction: (AppAction) -> Unit,
    isImporting: Boolean,
    onAddFilesClick: () -> Unit,
    refreshTrigger: Int
) {
    val context = LocalContext.current
    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current
    val coroutineScope = rememberCoroutineScope()

    var vaultFiles by remember { mutableStateOf(VaultManager.getVaultFiles(context)) }
    var selectedFileForMenu by remember { mutableStateOf<FileItem?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshTrigger) {
        vaultFiles = VaultManager.getVaultFiles(context)
    }

    fun refreshFiles() {
        vaultFiles = VaultManager.getVaultFiles(context)
    }

    val categories = remember {
        listOf(
            VaultCategoryItem("image", "Images", Icons.Outlined.Image, "images"),
            VaultCategoryItem("video", "Videos", Icons.Outlined.Videocam, "videos"),
            VaultCategoryItem("audio", "Audio", Icons.Outlined.MusicNote, "audio"),
            VaultCategoryItem("doc", "Docs", Icons.Outlined.Description, "docs"),
            VaultCategoryItem("apk", "Apps", Icons.Outlined.Android, "apps"),
            VaultCategoryItem("archive", "Archives", Icons.Outlined.FolderZip, "archives"),
            VaultCategoryItem("other", "Other", Icons.AutoMirrored.Outlined.InsertDriveFile, "large"),
            VaultCategoryItem("all", "All Files", Icons.Outlined.GridView, "downloads")
        )
    }

    fun matchesCategory(file: FileItem, key: String): Boolean {
        return when (key) {
            "image" -> file.type == "image"
            "video" -> file.type == "video"
            "audio" -> file.type == "audio"
            "doc" -> file.type == "doc"
            "apk" -> file.type == "apk"
            "archive" -> file.type == "archive"
            "other" -> file.type !in setOf("image", "video", "audio", "doc", "apk", "archive")
            "all" -> true
            else -> true
        }
    }

    fun getCategorySubtitle(key: String): String {
        val matches = vaultFiles.filter { matchesCategory(it, key) }
        if (matches.isEmpty()) return "0 items"
        val bytes = matches.sumOf { it.sizeBytes }
        return if (bytes > 0L) {
            android.text.format.Formatter.formatFileSize(context, bytes)
        } else {
            "${matches.size} items"
        }
    }

    val displayFiles = remember(vaultFiles, selectedCategory) {
        if (selectedCategory == null || selectedCategory == "all") {
            vaultFiles
        } else {
            vaultFiles.filter { matchesCategory(it, selectedCategory!!) }
        }
    }

    val totalBytes = remember(vaultFiles) { vaultFiles.sumOf { it.sizeBytes } }
    val formattedTotalSize = remember(totalBytes) {
        android.text.format.Formatter.formatFileSize(context, totalBytes)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CookieIconButton(
                        onClick = onClose,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        bgColor = colors.card,
                        iconTint = colors.text,
                        description = "Back and Lock",
                        size = 40.dp,
                        lobes = 8
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Vault",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalAppFont.current,
                            color = colors.text
                        )
                        Text(
                            text = "${vaultFiles.size} protected file${if (vaultFiles.size == 1) "" else "s"}",
                            fontSize = 12.sp,
                            fontFamily = LocalAppFont.current,
                            color = colors.muted
                        )
                    }
                }

                // Lock Vault button
                CookieIconButton(
                    onClick = {
                        haptics.tap()
                        onLock()
                    },
                    icon = Icons.Outlined.Lock,
                    bgColor = colors.card,
                    iconTint = colors.accent,
                    description = "Lock Vault",
                    size = 40.dp,
                    lobes = 8
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptics.tap()
                    onAddFilesClick()
                },
                containerColor = colors.accent,
                contentColor = colors.ink,
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Add Files",
                        tint = colors.ink
                    )
                },
                text = {
                    Text(
                        text = "Add Files",
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = LocalAppFont.current
                    )
                }
            )
        }
    ) { paddingValues ->
        if (isImporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.accent)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Overview Storage Card
                item {
                    SectionContainer(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CookieIcon(
                                icon = Icons.Outlined.Security,
                                bgColor = colors.accent,
                                iconTint = colors.ink,
                                size = 52.dp,
                                lobes = 10
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Encrypted Vault",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = LocalAppFont.current,
                                    color = colors.text
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Offline isolated storage • Invisible to other apps",
                                    fontSize = 12.sp,
                                    fontFamily = LocalAppFont.current,
                                    color = colors.muted
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        color = colors.card,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "$formattedTotalSize used",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = LocalAppFont.current,
                                            color = colors.text,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Surface(
                                        color = colors.card,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "${vaultFiles.size} protected",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = LocalAppFont.current,
                                            color = colors.text,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Categories Grid (similar to ExpressiveHomeScreen)
                item {
                    SectionContainer(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Categories",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = LocalAppFont.current,
                                    color = colors.text
                                )
                                if (selectedCategory != null) {
                                    Text(
                                        text = "Clear filter",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = LocalAppFont.current,
                                        color = colors.accent,
                                        modifier = Modifier.clickable {
                                            haptics.tap()
                                            selectedCategory = null
                                        }
                                    )
                                }
                            }

                            // Row 1 (4 items)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categories.take(4).forEach { cat ->
                                    VaultCategoryCookieTile(
                                        item = cat,
                                        subtitle = getCategorySubtitle(cat.key),
                                        isSelected = selectedCategory == cat.key,
                                        onClick = {
                                            haptics.tap()
                                            selectedCategory = if (selectedCategory == cat.key) null else cat.key
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Row 2 (4 items)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categories.drop(4).take(4).forEach { cat ->
                                    VaultCategoryCookieTile(
                                        item = cat,
                                        subtitle = getCategorySubtitle(cat.key),
                                        isSelected = if (cat.key == "all") selectedCategory == null else selectedCategory == cat.key,
                                        onClick = {
                                            haptics.tap()
                                            selectedCategory = if (cat.key == "all" || selectedCategory == cat.key) null else cat.key
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Section Title for Recent Files
                item {
                    val categoryTitle = categories.find { it.key == selectedCategory }?.title
                    val sectionTitle = if (categoryTitle != null && selectedCategory != "all") {
                        "$categoryTitle in Vault"
                    } else {
                        "Recent Files"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sectionTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalAppFont.current,
                            color = colors.text
                        )
                        Text(
                            text = "${displayFiles.size} items",
                            fontSize = 12.sp,
                            fontFamily = LocalAppFont.current,
                            color = colors.muted
                        )
                    }
                }

                // 4. File items or Empty State
                if (vaultFiles.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CookieIcon(
                                icon = Icons.Outlined.Security,
                                bgColor = colors.card,
                                iconTint = colors.accent,
                                size = 64.dp,
                                lobes = 10
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Your Vault is Empty",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = LocalAppFont.current,
                                color = colors.text,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Import private photos, videos, or documents here. Files are hidden from internal storage and other apps.",
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontFamily = LocalAppFont.current,
                                color = colors.muted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    haptics.tap()
                                    onAddFilesClick()
                                },
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = colors.ink
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = colors.ink,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Add Files to Vault",
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = LocalAppFont.current
                                )
                            }
                        }
                    }
                } else if (displayFiles.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No files found in this category",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = LocalAppFont.current,
                                color = colors.muted
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = { selectedCategory = null }) {
                                Text("Show all files", color = colors.accent, fontFamily = LocalAppFont.current)
                            }
                        }
                    }
                } else {
                    items(displayFiles, key = { it.path }) { file ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            VaultFileItemRow(
                                file = file,
                                onClick = {
                                    haptics.tap()
                                    onAction(AppAction.ViewFile(file))
                                },
                                onMenuClick = {
                                    haptics.tap()
                                    selectedFileForMenu = file
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // File Action Bottom Sheet
    if (selectedFileForMenu != null) {
        val targetFile = selectedFileForMenu!!
        ModalBottomSheet(
            onDismissRequest = { selectedFileForMenu = null },
            containerColor = colors.container,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = targetFile.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = LocalAppFont.current,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${targetFile.size} • ${targetFile.changed}",
                    fontSize = 12.sp,
                    fontFamily = LocalAppFont.current,
                    color = colors.muted
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action rows
                VaultActionTile(
                    icon = Icons.Outlined.FileOpen,
                    title = "Open / View",
                    onClick = {
                        selectedFileForMenu = null
                        onAction(AppAction.ViewFile(targetFile))
                    }
                )

                VaultActionTile(
                    icon = Icons.Outlined.SettingsBackupRestore,
                    title = "Restore to Internal Storage",
                    onClick = {
                        selectedFileForMenu = null
                        coroutineScope.launch(Dispatchers.IO) {
                            val success = VaultManager.restoreFile(context, targetFile)
                            withContext(Dispatchers.Main) {
                                if (success) {
                                    refreshFiles()
                                    onAction(AppAction.Reload)
                                    Toast.makeText(context, "Restored ${targetFile.name} to internal storage", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to restore file", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )

                VaultActionTile(
                    icon = Icons.Outlined.FileDownload,
                    title = "Export to Downloads",
                    onClick = {
                        selectedFileForMenu = null
                        val success = VaultManager.exportFile(context, targetFile)
                        if (success) {
                            Toast.makeText(context, "Exported ${targetFile.name} to Downloads", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to export file", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                VaultActionTile(
                    icon = Icons.Outlined.Share,
                    title = "Share",
                    onClick = {
                        selectedFileForMenu = null
                        try {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(targetFile.path))
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "*/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share ${targetFile.name}"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot share: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                VaultActionTile(
                    icon = Icons.Outlined.Delete,
                    title = "Delete Permanently",
                    titleColor = colors.accent,
                    iconTint = colors.accent,
                    onClick = {
                        selectedFileForMenu = null
                        VaultManager.deleteFile(context, targetFile)
                        refreshFiles()
                        Toast.makeText(context, "Deleted from Vault", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun VaultCategoryCookieTile(
    item: VaultCategoryItem,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    val pastel = ExpressiveTokens.categoryPastel(item.pastelKey)
    val roundness = LocalCornerRoundness.current
    val tileRadius = (22f * (roundness * 2).coerceIn(0f, 2f)).dp
    val tileShape = RoundedCornerShape(tileRadius)

    val isDark = colors.bg.luminance() < 0.5f
    val tintAlpha = if (isDark) 0.08f else 0.06f
    val tileBg = remember(pastel, colors.card, tintAlpha) {
        pastel.copy(alpha = tintAlpha).compositeOver(colors.card)
    }

    val borderColor = if (isSelected) colors.accent else colors.line.copy(alpha = colors.lineAlpha * 0.4f)
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(tileShape)
            .background(tileBg)
            .border(borderWidth, borderColor, tileShape)
            .pressScale(0.95f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp, horizontal = 2.dp)
    ) {
        CookieIcon(
            icon = item.icon,
            bgColor = pastel,
            size = 46.dp,
            lobes = cookieLobesForName(item.pastelKey)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = LocalAppFont.current,
            color = if (isSelected) colors.accent else colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = LocalAppFont.current,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun VaultFileItemRow(
    file: FileItem,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.line.copy(alpha = colors.lineAlpha)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveFileThumbnail(
                file = file,
                isSelected = false,
                size = 46.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = LocalAppFont.current,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${file.size} • ${file.changed}",
                    fontSize = 12.sp,
                    fontFamily = LocalAppFont.current,
                    color = colors.muted
                )
            }
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Options",
                    tint = colors.muted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun VaultActionTile(
    icon: ImageVector,
    title: String,
    titleColor: Color = ExpressiveTheme.colors.text,
    iconTint: Color = ExpressiveTheme.colors.text,
    onClick: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CookieIcon(
            icon = icon,
            bgColor = colors.card,
            iconTint = iconTint,
            size = 38.dp,
            lobes = 8
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = LocalAppFont.current,
            color = titleColor
        )
    }
}
