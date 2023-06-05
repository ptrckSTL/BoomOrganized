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
import com.simplemobiletools.boomorganized.UserSheetState
import com.simplemobiletools.boomorganized.sheets.UserSheet
import com.simplemobiletools.boomorganized.sheets.GoogleSheetSelectionContract
import com.simplemobiletools.boomorganized.rows

@Composable
fun BoomOrganizedGetCSVAndPreview(
    modifier: Modifier = Modifier,
    state: BoomOrganizedViewState.PreviewOutgoing,
    onAddCsv: () -> Unit,
    onGoogleSheetSelected: (UserSheet?) -> Unit,
) {
    var userSheetState by remember { mutableStateOf<UserSheet?>(null) }
    val launcher = rememberLauncherForActivityResult(GoogleSheetSelectionContract()) { result ->
        userSheetState = result
    }

    if (userSheetState != null) {
        onGoogleSheetSelected(userSheetState)
        userSheetState = null
    }
    Column(modifier = modifier.padding(horizontal = 12.dp)) {
        BOButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            text = if (state.userSheetState is UserSheetState.Found) {
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
        state.userSheetState.rows?.let { Text("Found ${state.userSheetState.rows} entries") }
        val preview = when (state.userSheetState) {
            is UserSheetState.Error -> state.userSheetState.msg
            is UserSheetState.Found -> state.preview
            UserSheetState.None -> state.preview
        }
        Spacer(Modifier.height(20.dp))
        RapWithImagePreview(preview = preview, photoUri = state.photoUri)
    }
}
