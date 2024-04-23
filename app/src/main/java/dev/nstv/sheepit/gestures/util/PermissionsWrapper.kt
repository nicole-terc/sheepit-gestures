package dev.nstv.sheepit.gestures.util

import android.Manifest
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

val defaultPermissions = listOf(
    Manifest.permission.ACTIVITY_RECOGNITION,
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsWrapper(
    modifier: Modifier = Modifier,
    permissions: List<String> = defaultPermissions,
    content: @Composable () -> Unit,
) {
    val permissionState: MultiplePermissionsState =
        rememberMultiplePermissionsState(permissions = permissions)

    if (permissionState.allPermissionsGranted) {
        content()
    } else {
        Column(
            modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "The following permissions are required for this app:",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = modifier.height(8.dp))
            permissions.forEach {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = modifier.height(8.dp))
            Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                Text("Grant permission", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}