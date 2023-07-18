@file:OptIn(ExperimentalFoundationApi::class)

package com.simplemobiletools.boomorganized.sheets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplemobiletools.boomorganized.BoomScaffold
import com.simplemobiletools.boomorganized.composables.BOButton
import com.simplemobiletools.boomorganized.formatDateTime
import com.simplemobiletools.boomorganized.ui.theme.Purple200
import com.simplemobiletools.boomorganized.ui.theme.Purple500
import com.simplemobiletools.boomorganized.ui.theme.Purple700

@Composable
fun SheetSelectionScreen(
    viewState: SheetSelectViewState,
    onSheetSelected: (SheetListItem) -> Unit,
    onSubSheetSelected: (ssID: String, subSheetName: String) -> Unit,
    onPrevious: () -> Unit,
    onForceClose: () -> Unit,
) {
    BoomScaffold(content = {
        BackHandler { onPrevious() }
        Spacer(Modifier.height(32.dp))
        when (viewState) {
            is SheetSelectViewState.ForceClose -> {
                onForceClose()
            }

            is SheetSelectViewState.Error -> {
                Box(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxSize()
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.Center), color = Color.White, text = viewState.msg ?: "Error!"
                    )
                }
            }

            is SheetSelectViewState.SheetsFound -> SheetListScreen(
                viewState = viewState, onSheetSelected = onSheetSelected
            )

            is SheetSelectViewState.SubSheetsFound -> SubSheetsFound(
                viewState, onSubSheetSelected = onSubSheetSelected
            )

            SheetSelectViewState.Uninitiated -> {
                Box(Modifier.fillMaxSize()) { ConditionalLoading(true) }
            }

            is SheetSelectViewState.Complete -> {}
        }
    }, navigation = {
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            when (viewState) {
                is SheetSelectViewState.Error,
                is SheetSelectViewState.SheetsFound,
                is SheetSelectViewState.SubSheetsFound,
                -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        BOButton(
                            onClick = onPrevious, text = if (viewState is SheetSelectViewState.SubSheetsFound) "Previous" else "Cancel"
                        )
                    }
                }

                else -> {}
            }
        }
    })
}

@Composable
fun SheetListScreen(viewState: SheetSelectViewState.SheetsFound, onSheetSelected: (SheetListItem) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        ConditionalLoading(viewState.isLoading)
        Column {
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
                items(viewState.sheets) {
                    SheetItem(modifier = if (!viewState.isLoading) Modifier.clickable {
                        onSheetSelected(it)
                    }
                    else Modifier, item = it)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun SubSheetsFound(
    viewState: SheetSelectViewState.SubSheetsFound,
    onSubSheetSelected: (ssID: String, subSheetName: String) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        ConditionalLoading(viewState.isLoading)
        Column {
            Spacer(Modifier.height(24.dp))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                textAlign = TextAlign.Start,
                fontSize = 24.sp,
                fontWeight = FontWeight.Thin,
                color = Color.White,
                text = "Select a tab"
            )
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                items(viewState.sheets) {
                    SheetItem(
                        modifier = Modifier.clickable { onSubSheetSelected(viewState.ssID, it) }, item = SheetListItem(
                            title = it, id = "", description = null, date = null
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
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
                fontSize = 18.sp, text = item.title, color = Color.White
            )
            Text(
                text = item.date?.value?.formatDateTime() ?: "", fontSize = 12.sp, color = Color.White
            )
        }
    }
}

@Composable
fun GridScreen(
    headers: List<String>,
    rows: List<List<String>>,
    firstNameIndex: Int,
    lastNameIndex: Int,
    cellIndex: Int, error: SheetError,
    onLabelSelected: (Int, ColumnLabel?) -> Unit,
    onCreateFilter: (Int) -> Unit,
) {
    val horizontalScrollState = rememberScrollState()
    Column {
        Text("Found ${rows.size} entries")
        Column(Modifier.weight(1f)) {
            LazyColumn {
                stickyHeader {
                    Row(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                        headers.forEachIndexed { index, s ->
                            HeaderCell(
                                text = s,
                                columnIndex = index,
                                label = columnLabel(index, cellIndex, lastNameIndex, firstNameIndex),
                                onLabelSelected = onLabelSelected,
                                onCreateFilter = onCreateFilter
                            )
                        }
                    }
                }
                items(rows) {
                    CellRow(
                        modifier = Modifier.horizontalScroll(state = horizontalScrollState),
                        row = it,
                        firstNameIndex = firstNameIndex,
                        lastNameIndex = lastNameIndex,
                        cellIndex = cellIndex,
                        onLabelSelected = onLabelSelected,
                        onCreateFilter = onCreateFilter
                    )
                }
            }
        }
        error.text?.let { errorText ->
            Row(
                modifier = Modifier
                    .padding()
                    .fillMaxWidth()
                    .background(Color.Red)
                    .padding(12.dp)
            ) {
                Text(
                    fontSize = 16.sp, text = errorText
                )
            }
        }
    }
}

@Composable
fun CellRow(
    modifier: Modifier = Modifier,
    row: List<String>,
    firstNameIndex: Int,
    lastNameIndex: Int,
    cellIndex: Int,
    onLabelSelected: (Int, ColumnLabel?) -> Unit,
    onCreateFilter: (Int) -> Unit
) {
    Row(modifier = modifier) {
        row.forEachIndexed { index, text ->
            SheetCell(
                text = text,
                columnIndex = index,
                borderColor = filterBorder(firstNameIndex, lastNameIndex, cellIndex, index),
                onLabelSelected = onLabelSelected,
                onCreateFilter = onCreateFilter
            )
        }
    }
}

@Composable
fun SheetCell(
    modifier: Modifier = Modifier,
    borderColor: Color,
    text: String,
    fontWeight: FontWeight = FontWeight.Normal,
    columnIndex: Int,
    width: Dp = 96.dp,
    height: Dp = 38.dp,
    onLabelSelected: (Int, ColumnLabel?) -> Unit,
    onCreateFilter: (Int) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        Column {
            ColumnLabel.values().forEach { label ->
                LabelSelectionRow(label = label, onSelected = {
                    showMenu = false
                    onLabelSelected(
                        columnIndex, label
                    )
                })
            }
            LabelSelectionRow(label = null, onSelected = {
                showMenu = false
                onLabelSelected(columnIndex, null)
            })
        }
    }

    Cell(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(width = 1.dp, color = borderColor)
            .padding(4.dp)
            .combinedClickable(onClick = { showMenu = true }, onLongClick = { onCreateFilter(columnIndex) }), text = text, fontWeight = fontWeight
    )
}

@Composable
fun Cell(modifier: Modifier = Modifier, text: String, fontWeight: FontWeight = FontWeight.Normal) {
    Text(
        modifier = modifier, color = Color.White, text = text, fontSize = 14.sp, fontWeight = fontWeight
    )
}

@Composable
fun HeaderCell(
    modifier: Modifier = Modifier,
    text: String,
    columnIndex: Int,
    label: ColumnLabel?,
    onLabelSelected: (Int, ColumnLabel?) -> Unit,
    onCreateFilter: (Int) -> Unit
) {
    Box(modifier = modifier.background(Color.DarkGray)) {
        SheetCell(
            modifier = modifier.background(Color.DarkGray),
            height = 40.dp,
            borderColor = Color.White,
            fontWeight = FontWeight.ExtraBold,
            text = text,
            columnIndex = columnIndex,
            onLabelSelected = onLabelSelected,
            onCreateFilter = onCreateFilter
        )
        label?.let {
            Row(
                Modifier
                    .shadow(4.dp, shape = RoundedCornerShape(12.dp))
                    .padding(2.dp)
                    .align(Alignment.Center)
                    .background(it.color, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Text(
                    color = Color.White, text = it.text
                )
            }
        }
    }
}

/**
 * one row of the drop down menu
 */
@Composable
fun LabelSelectionRow(label: ColumnLabel?, onSelected: (ColumnLabel?) -> Unit) {
    Divider()
    Column(
        Modifier
            .width(200.dp)
            .clickable { onSelected(label) }) {
        Text(
            fontSize = 18.sp, text = label?.text ?: "None", modifier = Modifier
                .padding(4.dp)
                .width(200.dp)
        )
        Divider(thickness = Dp.Hairline)
    }
}

@Composable
fun BoxScope.ConditionalLoading(isLoading: Boolean) {
    if (isLoading) {
        Box(
            Modifier
                .background(color = Color.DarkGray, shape = CircleShape)
                .align(Alignment.Center)
                .padding(4.dp)
        ) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
    }
}

@Composable
fun filterBorder(firstNameIndex: Int, lastNameIndex: Int, cellIndex: Int, index: Int) = when {
    firstNameIndex == index -> ColumnLabel.FirstName.color
    lastNameIndex == index -> ColumnLabel.LastName.color
    cellIndex == index -> ColumnLabel.CellPhone.color
    else -> Color.White
}

val ColumnLabel.text: String
    get() = when (this) {
        ColumnLabel.FirstName -> "First name"
        ColumnLabel.LastName -> "Last name"
        ColumnLabel.CellPhone -> "Cell phone"
    }
val ColumnLabel.color: Color
    @Composable get() = when (this) {
        ColumnLabel.FirstName -> Purple500
        ColumnLabel.LastName -> Purple700
        ColumnLabel.CellPhone -> Purple200
    }

fun columnLabel(index: Int, cellIndex: Int, lastNameIndex: Int, firstNameIndex: Int): ColumnLabel? = when (index) {
    cellIndex -> ColumnLabel.CellPhone
    firstNameIndex -> ColumnLabel.FirstName
    lastNameIndex -> ColumnLabel.LastName
    else -> null
}

val SheetError.text: String?
    get() = when (this) {
        SheetError.NoCellSelected -> "A label for the cell column is required"
        SheetError.NoFirstName -> "A firstName template exists in your rap but no label was found. Label the first name column or go back and edit your rap."
        SheetError.NoLastName -> "You didn't select a last name, but the lastName template exists in your rap. Select a last name column or go back and edit your rap."
        SheetError.None -> null
        is SheetError.SomeCellEntriesMissing -> "$this rows have a missing or malformed cell phone number. These will be purged from the rolls. If that's okay hit next again."
    }
