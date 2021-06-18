
package com.vitorpamplona.amethyst.ui.actions

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.vitorpamplona.amethyst.R

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun UploadFromGallery(
    isUploading: Boolean,
    onImageChosen: (Uri) -> Unit
) {
    val cameraPermissionState =
        rememberPermissionState(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }
        )

    if (cameraPermissionState.status.isGranted) {
        var showGallerySelect by remember { mutableStateOf(false) }
        if (showGallerySelect) {
            GallerySelect(
                onImageUri = { uri ->
                    showGallerySelect = false
                    if (uri != null) {
                        onImageChosen(uri)
                    }
                }
            )
        } else {
            UploadBoxButton(isUploading) {
                showGallerySelect = true
            }
        }
    } else {
        UploadBoxButton(isUploading) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
}

@Composable
private fun UploadBoxButton(
    isUploading: Boolean,
    onClick: () -> Unit
) {
    Box() {
        TextButton(
            modifier = Modifier
                .align(Alignment.TopCenter),
            enabled = !isUploading,
            onClick = {
                onClick()
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_photo),
                contentDescription = stringResource(id = R.string.upload_image),
                modifier = Modifier
                    .height(20.dp)
                    .padding(end = 8.dp),
                tint = MaterialTheme.colors.primary
            )

            if (!isUploading) {
                Text(stringResource(R.string.upload_image))
            } else {
                Text(stringResource(R.string.uploading))
            }
        }
    }
}

@Composable
fun GallerySelect(
    onImageUri: (Uri?) -> Unit = { }
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            onImageUri(uri)
        }
    )

    @Composable
    fun LaunchGallery() {
        SideEffect {
            launcher.launch("image/*")
        }
    }

    LaunchGallery()
}