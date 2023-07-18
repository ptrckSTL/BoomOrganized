package com.simplemobiletools.boomorganized.sheets

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// TODO someday
@Composable
fun SheetValueFilterScreen(
//    filterViewState: SheetSelectViewState.FilterSetup,
    addToInclude: (Int, String) -> Unit,
    addToExclude: (Int, String) -> Unit
) {
    LazyColumn {
//        item(filterViewState.header) {
//            Cell(text = filterViewState.header)
//        }
//        itemsIndexed(filterViewState.values) { index, item ->
//            FilterableCell(
//                value = item,
//                addToInclude = { addToInclude(index, item) },
//                addToExclude = { addToExclude(index, item) }
//            )
//        }
    }
}

@Composable
fun FilterableCell(
    modifier: Modifier = Modifier,
    value: String,
    addToInclude: (String) -> Unit,
    addToExclude: (String) -> Unit
) {

    val plus: ImageVector = remember { Icons.Default.Add }
    val minus: ImageVector = remember { Icons.Default.Delete }
//    val borderColor = when (value) {
//        is FilterableValue.Exclude -> Color.Red
//        is FilterableValue.Include -> Color.Green
//        is FilterableValue.None -> Color.White
//    }

    Row(
        modifier = modifier.border(1.dp, Color.White),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = plus,
            contentDescription = plus.name,
            modifier = Modifier
                .size(24.dp)
                .clickable { addToInclude(value) }
        )
        Text(
            text = value,
            modifier = Modifier.width(80.dp)
        )
        Icon(
            imageVector = minus,
            contentDescription = minus.name,
            modifier = Modifier
                .size(24.dp)
                .clickable { addToExclude(value) }
        )
    }
}
