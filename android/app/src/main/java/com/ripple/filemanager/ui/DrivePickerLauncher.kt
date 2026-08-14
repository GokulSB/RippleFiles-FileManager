package com.ripple.filemanager.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

/**
 * Current (2026) supported way to show the Google Drive Picker on Android.
 *
 * This replaces the old embedded-WebView + picker.js approach, which Google
 * no longer supports for desktop/mobile apps. The Picker is now driven
 * through the Identity API's AuthorizationRequest: it opens as part of the
 * OAuth consent flow itself (system handles the UI), and hands back the
 * IDs of whatever the user picked.
 *
 * Only `drive.file` is permitted here — it cannot be combined with any
 * other scope for this flow.
 */
@Composable
fun rememberDrivePickerLauncher(
    onResult: (success: Boolean, pickedIds: List<String>?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val authorizationClient = remember { Identity.getAuthorizationClient(context) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            try {
                val result = authorizationClient
                    .getAuthorizationResultFromIntent(activityResult.data)
                
                var pickedIds: List<String>? = null
                try {
                    val params = result.javaClass.getMethod("getTokenResponseParams").invoke(result) as? android.os.Bundle
                    val idsCsv = params?.getString("picked_file_ids")
                    pickedIds = idsCsv?.split(",")?.filter { it.isNotBlank() }
                } catch(e: Exception) {}
                
                if (pickedIds == null && activityResult.data?.hasExtra("picked_file_ids") == true) {
                    pickedIds = activityResult.data?.getStringExtra("picked_file_ids")?.split(",")?.filter { it.isNotBlank() }
                }
                
                onResult(true, pickedIds)
            } catch (e: Exception) {
                onResult(false, null)
            }
        } else {
            onResult(false, null) // user cancelled
        }
    }

    return {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DriveScopes.DRIVE_FILE)))
            .setOptOutIncludingGrantedScopes(true)
            .addResourceParameter(AuthorizationRequest.ResourceParameter.PICKER_OAUTH_TRIGGER, "true")
            .build()

        authorizationClient.authorize(request)
            .addOnSuccessListener { authResult: AuthorizationResult ->
                if (authResult.hasResolution()) {
                    // Launches the system-browser-hosted consent + Picker flow.
                    val pendingIntent = authResult.pendingIntent
                    val intentSenderRequest =
                        androidx.activity.result.IntentSenderRequest
                            .Builder(pendingIntent!!.intentSender)
                            .build()
                    launcher.launch(intentSenderRequest)
                } else {
                    // Already authorized with drive.file — no picker needed,
                    // e.g. a previously granted, still-valid selection.
                    onResult(true, null)
                }
            }
            .addOnFailureListener {
                onResult(false, null)
            }
    }
}
