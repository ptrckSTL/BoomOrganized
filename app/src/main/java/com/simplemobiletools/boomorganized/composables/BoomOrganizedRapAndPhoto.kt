package com.simplemobiletools.boomorganized.composables

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BoomOrganizedRapAndPhoto(
    modifier: Modifier = Modifier,
    script: String,
    imageUri: Uri?,
    onAddPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onScriptEdit: (String) -> Unit,
) {
    Column(
        modifier = modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Column(verticalArrangement = Arrangement.Top) {
            Text(
                modifier = Modifier.padding(vertical = 12.dp),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                text = listOf(
                    "If you ain't cheating you ain't tryin",
                    "Everything is an organizing conversation",
                    "What's your rap?",
                    "Everything is a structure test",
                ).random()
            )
        }
        Spacer(Modifier.height(12.dp))
        EditScriptBox(
            true,
            savedScript = script,
            onScriptEdit = onScriptEdit
        )
        ScriptInformation()
        Attachment(
            modifier = Modifier
                .padding(top = 16.dp)
                .weight(1f),
            uri = imageUri,
            onAddAttachment = onAddPhoto,
            onRemoveAttachment = onRemovePhoto
        )
    }
}