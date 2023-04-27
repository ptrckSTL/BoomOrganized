package com.simplemobiletools.boomorganized.composables

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
fun BoomOrganizedResumePending(
    modifier: Modifier = Modifier,
    counts: ContactCounts,
    preview: String,
    imageUri: Uri?,
    onReattach: () -> Unit,
    onRemovePhoto: () -> Unit,
    onScriptEdit: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(fontSize = 18.sp, text = "⚠️")
            }
            Column {
                Text("Found ${counts.pending} pending messages from a previous session.")
                if (counts.sending != 0) {
                    Text("Also found ${counts.sending} messages still \"Sending\". These will be skipped.")
                }
            }
        }
        Column(modifier = Modifier.padding(top = 20.dp)) {
            Text(fontSize = 10.sp, text = "You may edit your message before you resume.")
            EditScriptBox(
                editable = true,
                savedScript = preview,
                onScriptEdit = onScriptEdit
            )
            Attachment(modifier = Modifier.padding(top = 12.dp), imageUri, onAddAttachment = onReattach, onRemoveAttachment = onRemovePhoto)
        }
    }
}