package com.simplemobiletools.boomorganized

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.google.common.collect.ImmutableList
import com.simplemobiletools.boomorganized.sheets.ColumnLabel
import com.simplemobiletools.boomorganized.sheets.UserSheet
import okhttp3.internal.toImmutableList

@Immutable
data class FilterableUserSheet(
    val headers: List<String>,
    val rows: List<List<String>>,
    val firstNameIndex: Int,
    val lastNameIndex: Int,
    val cellIndex: Int,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.createStringArrayList() ?: emptyList(), readList(parcel), parcel.readInt(), parcel.readInt(), parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeStringList(headers)
        parcel.writeInt(rows.size)
        for (row in rows) {
            parcel.writeStringList(row)
        }
        parcel.writeInt(firstNameIndex)
        parcel.writeInt(lastNameIndex)
        parcel.writeInt(cellIndex)
    }


    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FilterableUserSheet> {

        private fun readList(parcel: Parcel): List<List<String>> {
            val size = parcel.readInt()
            val list = ArrayList<List<String>>(size)
            for (i in 0 until size) {
                val sublist = parcel.createStringArrayList()
                if (sublist != null) {
                    list.add(sublist)
                }
            }
            return list
        }

        override fun createFromParcel(parcel: Parcel): FilterableUserSheet {
            return FilterableUserSheet(parcel)
        }

        override fun newArray(size: Int): Array<FilterableUserSheet?> {
            return arrayOfNulls(size)
        }

        fun fromRows(rows: List<List<String>>): FilterableUserSheet = UserSheet(rows).toFilterableDriveSheet()
    }
}

fun FilterableUserSheet.update(label: ColumnLabel?, index: Int) = when (label) {
    ColumnLabel.FirstName -> {
        val updateLastName = if (lastNameIndex == index) -1 else this.lastNameIndex
        val updateCellIndex = if (cellIndex == index) -1 else this.cellIndex
        copy(
            lastNameIndex = updateLastName, cellIndex = updateCellIndex, firstNameIndex = index
        )
    }

    ColumnLabel.LastName -> {
        val updateFirstName = if (firstNameIndex == index) -1 else this.firstNameIndex
        val updateCellIndex = if (cellIndex == index) -1 else this.cellIndex
        copy(
            firstNameIndex = updateFirstName, cellIndex = updateCellIndex, lastNameIndex = index
        )
    }

    ColumnLabel.CellPhone -> {
        val updateLastName = if (lastNameIndex == index) -1 else this.lastNameIndex
        val updateFirstName = if (firstNameIndex == index) -1 else this.firstNameIndex
        copy(
            lastNameIndex = updateLastName, firstNameIndex = updateFirstName, cellIndex = index
        )
    }

    null -> {
        val updateFirstName = if (firstNameIndex == index) -1 else this.firstNameIndex
        val updateCellIndex = if (cellIndex == index) -1 else this.cellIndex
        val updateLastName = if (lastNameIndex == index) -1 else this.lastNameIndex
        copy(
            firstNameIndex = updateFirstName, cellIndex = updateCellIndex, lastNameIndex = updateLastName
        )
    }
}

fun UserSheet.toFilterableDriveSheet(): FilterableUserSheet {
    val filterableRows = buildList {
        rows.forEachIndexed { index, row ->
            if (index != 0) {
                add(row)
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
        defaultFirstNameIndex, defaultLastNameIndex, defaultCellIndex
    )
}

object SpreadsheetParsingException : Exception()
