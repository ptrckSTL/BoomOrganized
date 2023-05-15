package com.simplemobiletools.boomorganized.composables

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.simplemobiletools.boomorganized.BoomOrganizedViewState
import com.simplemobiletools.boomorganized.CsvState
import com.simplemobiletools.boomorganized.oauth.DriveSheet
import com.simplemobiletools.boomorganized.oauth.GoogleSheetSelectionContract
import com.simplemobiletools.boomorganized.rows

@Composable
fun BoomOrganizedGetCSVAndPreview(
    modifier: Modifier = Modifier,
    state: BoomOrganizedViewState.CsvAndPreview,
    onAddCsv: () -> Unit,
    onGoogleSheetSelected: (DriveSheet?) -> Unit,
) {
    var driveSheetState by remember { mutableStateOf<DriveSheet?>(null) }
    val launcher = rememberLauncherForActivityResult(GoogleSheetSelectionContract()) { result ->
        driveSheetState = result
    }

    if (driveSheetState != null) {
        onGoogleSheetSelected(driveSheetState)
        driveSheetState = null
    }
    Column(modifier = modifier.padding(horizontal = 12.dp)) {
        BOButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            text = if (state.csvState is CsvState.Found) {
                "Choose a different CSV"
            } else {
                "Choose a CSV"
            },
            onClick = onAddCsv,
        )
        BOButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            text = "Use a Google Sheet",
            onClick = { launcher.launch(Unit) }
        )
        state.csvState.rows?.let { Text("Found ${state.csvState.rows} entries") }
        val preview = when (state.csvState) {
            is CsvState.Error -> state.csvState.msg
            is CsvState.Found -> state.preview
            CsvState.None -> state.preview
        }
        Spacer(Modifier.height(20.dp))
        RapWithImagePreview(preview = preview, photoUri = state.photoUri)
    }
}