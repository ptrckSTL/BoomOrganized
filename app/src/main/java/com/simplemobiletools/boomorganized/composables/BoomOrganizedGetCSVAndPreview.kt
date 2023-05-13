package com.simplemobiletools.boomorganized.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.simplemobiletools.boomorganized.BoomOrganizedViewState
import com.simplemobiletools.boomorganized.CsvState
import com.simplemobiletools.boomorganized.rows

@Composable
fun BoomOrganizedGetCSVAndPreview(
    modifier: Modifier = Modifier,
    state: BoomOrganizedViewState.CsvAndPreview,
    onAddCsv: () -> Unit,
) {
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