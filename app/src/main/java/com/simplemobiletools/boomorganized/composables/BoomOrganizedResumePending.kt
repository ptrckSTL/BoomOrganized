package com.simplemobiletools.boomorganized.composables

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplemobiletools.boomorganized.ContactCounts

@Composable
fun ColumnScope.BoomOrganizedResumePending(
    counts: ContactCounts,
    preview: String,
    imageUri: Uri?,
    onResume: () -> Unit,
    onDecline: () -> Unit,
    onReattach: () -> Unit,
    onRemovePhoto: () -> Unit,
    onScriptEdit: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 12.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Found ${counts.pending} pending messages from a previous session. Resume?")
        if (counts.sending != 0) {
            Text("Also found ${counts.sending} messages still \"Sending\". These will be skipped.")
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            BOButton(
                modifier = Modifier.align(Alignment.BottomEnd),
                text = "Resume previous session",
                onClick = onResume
            )
            BOButton(
                modifier = Modifier.align(Alignment.BottomStart),
                onClick = onDecline,
                text = "Start fresh"
            )
        }
        Column {
            Text(fontSize = 12.sp, text = "You may edit your message before you resume.")
            EditScriptBox(
                editable = true,
                savedScript = preview,
                onScriptEdit = onScriptEdit
            )
            Attachment(modifier = Modifier, imageUri, onAddAttachment = onReattach, onRemoveAttachment = onRemovePhoto)
            BOButton(
                modifier = Modifier.align(Alignment.End),
                onClick = onReattach,
                text = imageUri?.let { "Attach a different image" } ?: "Attach an image"
            )
        }
    }
}