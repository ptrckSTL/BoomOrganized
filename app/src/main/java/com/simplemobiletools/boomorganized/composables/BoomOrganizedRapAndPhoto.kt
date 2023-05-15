package com.simplemobiletools.boomorganized.composables

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val phrase = remember(Unit) {
        if ((1..50).random() == 1) "a square is just a circle" else
            listOf(
                "if you ain't cheating you ain't tryin",
                "everything is an organizing conversation",
                "what's your rap?",
                "everything is a structure test",
                "always be organizing",
            ).random()
    }
    Column(
        modifier = modifier.padding(start = 8.dp, end = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier
                .padding(top = 4.dp, bottom = 12.dp)
                .fillMaxWidth(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Thin,
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic,
            text = phrase
        )
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