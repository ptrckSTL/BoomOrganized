package com.simplemobiletools.boomorganized.oauth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplemobiletools.boomorganized.BoomScaffold
import com.simplemobiletools.boomorganized.formatDateTime

@Composable
fun SheetSelectionScreen(
    viewState: SheetSelectViewState,
    onSheetSelected: (SheetListItem) -> Unit,
    onSubSheetSelected: (ssID: String, subSheetName: String) -> Unit,
) {
    BoomScaffold {
        when (viewState) {
            is SheetSelectViewState.Error -> Text(color = Color.White, text = viewState.msg ?: "Error!")
            is SheetSelectViewState.SheetSelected -> GridScreen(sheet = viewState.sheet)
            is SheetSelectViewState.SheetsFound -> SheetListScreen(listings = viewState.sheets, onSheetSelected = onSheetSelected)
            SheetSelectViewState.Uninitiated -> {}
            is SheetSelectViewState.SubSheetsFound -> SubSheetsFound(
                subSheetID = viewState.ssID,
                subSheets = viewState.sheets,
                onSubSheetSelected = onSubSheetSelected
            )
        }
    }
}

@Composable
fun SheetListScreen(listings: List<SheetListItem>, onSheetSelected: (SheetListItem) -> Unit) {
    Spacer(Modifier.height(24.dp))
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        textAlign = TextAlign.Start,
        fontSize = 24.sp,
        fontWeight = FontWeight.Thin,
        color = Color.White,
        text = "Select a sheet"
    )
    LazyColumn(modifier = Modifier.padding(8.dp)) {
        items(listings) {
            SheetItem(
                modifier = Modifier.clickable { onSheetSelected(it) }, item = it
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
fun SubSheetsFound(
    subSheetID: String,
    subSheets: List<String>,
    onSubSheetSelected: (ssID: String, subSheetName: String) -> Unit,
) {
    LazyColumn(modifier = Modifier.padding(8.dp)) {
        items(subSheets) {
            SheetItem(
                modifier = Modifier.clickable { onSubSheetSelected(subSheetID, it) },
                item = SheetListItem(
                    title = it,
                    id = "",
                    description = null,
                    date = null
                )
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
fun SheetItem(modifier: Modifier = Modifier, item: SheetListItem) {
    Row(
        modifier = modifier
            .border(BorderStroke(Dp.Hairline, Color.White), RoundedCornerShape(2.dp))
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Column {
            Text(
                fontSize = 18.sp,
                text = item.title,
                color = Color.White
            )
            Text(
                text = item.date?.value?.formatDateTime() ?: "",
                fontSize = 12.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun GridScreen(sheet: DriveSheet) {
    LazyVerticalGrid(columns = GridCells.Fixed(sheet.headers.size)) {
        // the first row needs the header appended
        items(sheet.headers.size) {
            Text(
                modifier = Modifier.border(width = Dp.Hairline, color = Color.White),
                color = Color.White,
                text = sheet.headers.getOrNull(it) ?: "",
                fontWeight = FontWeight.Bold
            )
        }
        items(sheet.flatRows.size) {
            Text(
                modifier = Modifier.border(width = Dp.Hairline, color = Color.White),
                color = Color.White,
                text = sheet.flatRows[it]
            )
        }
    }
}