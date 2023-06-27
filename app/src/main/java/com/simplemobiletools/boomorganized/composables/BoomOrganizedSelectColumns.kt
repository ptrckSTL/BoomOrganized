package com.simplemobiletools.boomorganized.composables

import androidx.compose.runtime.Composable
import com.simplemobiletools.boomorganized.BoomOrganizedViewState
import com.simplemobiletools.boomorganized.sheets.ColumnLabel
import com.simplemobiletools.boomorganized.sheets.GridScreen

@Composable
fun BoomOrganizedSelectColumns(
    viewState: BoomOrganizedViewState.RequestLabels,
    onLabelSelected: (Int, ColumnLabel?) -> Unit,
    onCreateFilter: (Int) -> Unit
) {
    GridScreen(
        error = viewState.error,
        onLabelSelected = onLabelSelected,
        onCreateFilter = onCreateFilter,
        headers = viewState.sheet.headers,
        rows = viewState.sheet.rows,
        firstNameIndex = viewState.sheet.firstNameIndex,
        lastNameIndex = viewState.sheet.lastNameIndex,
        cellIndex = viewState.sheet.cellIndex
    )
}
