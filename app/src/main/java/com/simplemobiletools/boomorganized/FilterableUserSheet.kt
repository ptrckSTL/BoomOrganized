package com.simplemobiletools.boomorganized

import com.simplemobiletools.boomorganized.sheets.ColumnLabel
import com.simplemobiletools.boomorganized.sheets.FilterableValue
import com.simplemobiletools.boomorganized.sheets.UserSheet

data class FilterableUserSheet(
    val headers: List<String>,
    val rows: List<List<FilterableValue>>,
    val firstNameIndex: Int,
    val lastNameIndex: Int,
    val cellIndex: Int,
) {
    companion object {
        fun fromRows(rows: List<List<String>>): FilterableUserSheet =
            UserSheet(rows).toFilterableDriveSheet()
    }
}


fun FilterableUserSheet.update(label: ColumnLabel?, index: Int) = when (label) {
    ColumnLabel.FirstName -> {
        val updateLastName = if (lastNameIndex == index) -1 else this.lastNameIndex
        val updateCellIndex = if (cellIndex == index) -1 else this.cellIndex
        copy(
            lastNameIndex = updateLastName,
            cellIndex = updateCellIndex,
            firstNameIndex = index
        )
    }

    ColumnLabel.LastName -> {
        val updateFirstName = if (firstNameIndex == index) -1 else this.firstNameIndex
        val updateCellIndex = if (cellIndex == index) -1 else this.cellIndex
        copy(
            firstNameIndex = updateFirstName,
            cellIndex = updateCellIndex,
            lastNameIndex = index
        )
    }

    ColumnLabel.CellPhone -> {
        val updateLastName = if (lastNameIndex == index) -1 else this.lastNameIndex
        val updateFirstName = if (firstNameIndex == index) -1 else this.firstNameIndex
        copy(
            lastNameIndex = updateLastName,
            firstNameIndex = updateFirstName,
            cellIndex = index
        )
    }

    null -> {
        val updateFirstName = if (firstNameIndex == index) -1 else this.firstNameIndex
        val updateCellIndex = if (cellIndex == index) -1 else this.cellIndex
        val updateLastName = if (lastNameIndex == index) -1 else this.lastNameIndex
        copy(
            firstNameIndex = updateFirstName,
            cellIndex = updateCellIndex,
            lastNameIndex = updateLastName
        )
    }
}

fun UserSheet.toFilterableDriveSheet(): FilterableUserSheet {
    val filterableRows = buildList<List<FilterableValue>> {
        rows.forEachIndexed { index, row ->
            if (index != 0) {
                add(row.map { FilterableValue.None(it) })
            }
        }
    }
    val (first, last, cell) = detectColumns(this.rows.first())
    return FilterableUserSheet(
        headers = this.rows.first(),
        rows = filterableRows,
        firstNameIndex = first,
        lastNameIndex = last,
        cellIndex = cell,
    )
}

fun detectColumns(header: List<String>): Triple<Int, Int, Int> {
    var defaultFirstNameIndex = -1
    var defaultLastNameIndex = -1
    var defaultCellIndex = -1

    header.forEachIndexed { index, s ->
        when {
            Regex("(?i)first( name)?|firstName").matches(s) -> defaultFirstNameIndex = index
            Regex("(?i)last( name)?|lastName").matches(s) -> defaultLastNameIndex = index
            Regex("(?i)cell(ular)?|phone(1|\\s)?|mobile").matches(s) -> defaultCellIndex = index
        }
    }
    return Triple(
        defaultFirstNameIndex,
        defaultLastNameIndex,
        defaultCellIndex
    )
}

object SpreadsheetParsingException : Exception()
