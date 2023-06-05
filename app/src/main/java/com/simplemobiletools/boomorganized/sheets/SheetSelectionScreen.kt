@file:OptIn(ExperimentalFoundationApi::class)

package com.simplemobiletools.boomorganized.sheets

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
import com.simplemobiletools.boomorganized.FilterableUserSheet
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
    onLabelSelected: (Int, ColumnLabel?) -> Unit,
    onAddInclusiveFilter: (index: Int, value: String) -> Unit,
    onAddExclusiveFilter: (index: Int, value: String) -> Unit,
    onCreateFilter: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onForceClose: () -> Unit,
) {
    BoomScaffold(
        content = {
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
                    )
                    {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White,
                            text = viewState.msg ?: "Error!"
                        )
                    }
                }

                is SheetSelectViewState.SheetSelected -> GridScreen(
                    grid = viewState.sheet,
                    error = viewState.error,
                    onLabelSelected = onLabelSelected,
                    onCreateFilter = onCreateFilter,
                )

                is SheetSelectViewState.SheetsFound -> SheetListScreen(
                    viewState = viewState,
                    onSheetSelected = onSheetSelected
                )

                is SheetSelectViewState.SubSheetsFound -> SubSheetsFound(
                    viewState,
                    onSubSheetSelected = onSubSheetSelected
                )

                SheetSelectViewState.Uninitiated -> {
                    Box(Modifier.fillMaxSize()) { ConditionalLoading(true) }
                }

                is SheetSelectViewState.Complete -> {}
                is SheetSelectViewState.FilterSetup -> SheetValueFilterScreen(
                    filterViewState = viewState,
                    addToInclude = onAddInclusiveFilter,
                    addToExclude = onAddExclusiveFilter
                )
            }
        },
        navigation = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                when (viewState) {
                    is SheetSelectViewState.SheetSelected -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            BOButton(
                                onClick = onPrevious,
                                text = "Previous"
                            )

                            BOButton(
                                text = "Next",
                                onClick = onNext
                            )
                        }
                    }

                    is SheetSelectViewState.Error,
                    is SheetSelectViewState.SheetsFound,
                    is SheetSelectViewState.SubSheetsFound,
                    -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            BOButton(
                                onClick = onPrevious,
                                text = if (viewState is SheetSelectViewState.SubSheetsFound) "Previous" else "Cancel"
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    )
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
                    SheetItem(
                        modifier = if (!viewState.isLoading) Modifier.clickable {
                            onSheetSelected(it)
                        }
                        else Modifier,
                        item = it
                    )
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
                        modifier = Modifier.clickable { onSubSheetSelected(viewState.ssID, it) },
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
fun GridScreen(
    grid: FilterableUserSheet,
    error: SheetError,
    onLabelSelected: (Int, ColumnLabel?) -> Unit,
    onCreateFilter: (Int) -> Unit,
) {
    Column {
        Text("Found ${grid.rows.size} entries, displaying first 10")
        Column(
            Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState())
        ) {
            Row {
                grid.headers.forEachIndexed { index, text ->
                    Column {
                        HeaderCell(
                            text = text,
                            columnIndex = index,
                            label = columnLabel(index, grid),
                            onLabelSelected = onLabelSelected,
                            onCreateFilter = onCreateFilter
                        )
                    }
                }
            }
            grid.rows.take(10).forEach { row ->
                Row {
                    row.forEachIndexed { index, text ->
                        SheetCell(
                            text = text.value,
                            columnIndex = index,
                            borderColor = filterBorder(grid, index),
                            onLabelSelected = onLabelSelected,
                            onCreateFilter = onCreateFilter
                        )
                    }
                }
            }
        }

        Row {
            error.text?.let { Text(it) }
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
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        Column {
            ColumnLabel.values().forEach { label ->
                LabelSelectionRow(
                    label = label,
                    onSelected = {
                        showMenu = false
                        onLabelSelected(
                            columnIndex,
                            label
                        )
                    }
                )
            }
            LabelSelectionRow(
                label = null,
                onSelected = {
                    showMenu = false
                    onLabelSelected(columnIndex, null)
                }
            )
        }
    }

    Cell(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(width = 1.dp, color = borderColor)
            .padding(4.dp)
            .combinedClickable(
                onClick = { showMenu = true },
                onLongClick = { onCreateFilter(columnIndex) }
            ),
        text = text,
        fontWeight = fontWeight
    )
}

@Composable
fun Cell(modifier: Modifier = Modifier, text: String, fontWeight: FontWeight = FontWeight.Normal) {
    Text(
        modifier = modifier,
        color = Color.White,
        text = text,
        fontWeight = fontWeight
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
    Box {
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
                    color = Color.White,
                    text = it.text
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
            .clickable { onSelected(label) }
    ) {
        Text(
            fontSize = 18.sp,
            text = label?.text ?: "None",
            modifier = Modifier
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
fun filterBorder(userSheet: FilterableUserSheet, index: Int) =
    when {
        userSheet.firstNameIndex == index -> ColumnLabel.FirstName.color
        userSheet.lastNameIndex == index -> ColumnLabel.LastName.color
        userSheet.cellIndex == index -> ColumnLabel.CellPhone.color
        else -> Color.White
    }

val ColumnLabel.text: String
    get() = when (this) {
        ColumnLabel.FirstName -> "First name"
        ColumnLabel.LastName -> "Last name"
        ColumnLabel.CellPhone -> "Cell phone"
    }
val ColumnLabel.color: Color
    @Composable
    get() = when (this) {
        ColumnLabel.FirstName -> Purple500
        ColumnLabel.LastName -> Purple700
        ColumnLabel.CellPhone -> Purple200
    }

fun columnLabel(index: Int, userSheet: FilterableUserSheet): ColumnLabel? =
    when (index) {
        userSheet.cellIndex -> ColumnLabel.CellPhone
        userSheet.firstNameIndex -> ColumnLabel.FirstName
        userSheet.lastNameIndex -> ColumnLabel.LastName
        else -> null
    }

val SheetError.text: String?
    get() = when (this) {
        SheetError.NoCellSelected -> "A label for the cell column is required"
        SheetError.NoFirstName -> "You didn't select a first name. If that's okay hit next again."
        SheetError.NoLastName -> "You didn't select a last name. If that's okay hit next again."
        SheetError.None -> null
        is SheetError.SomeCellEntriesMissing -> "$this rows have a missing or malformed cell phone number. These will be purged from the rolls. If that's okay hit next again."
    }
