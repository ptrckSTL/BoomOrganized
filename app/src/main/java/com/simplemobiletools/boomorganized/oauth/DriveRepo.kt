package com.simplemobiletools.boomorganized.oauth

import android.accounts.Account
import android.os.Parcel
import android.os.Parcelable
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.simplemobiletools.smsmessenger.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

object DriveRepo {
    private val httpTransport = NetHttpTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val sheets by lazy {
        Sheets.Builder(httpTransport, jsonFactory, credential()).build()
    }
    var selectedAccount: Account? = null

    private fun credential(): GoogleAccountCredential? {
        val scopes = listOf(SheetsScopes.SPREADSHEETS_READONLY, DriveScopes.DRIVE_READONLY)
        val credential = GoogleAccountCredential.usingOAuth2(App.instance, scopes)
        credential.selectedAccount = selectedAccount
        return credential
    }

    suspend fun getListOfFiles(sinceWhen: Date = Date(), searchByName: String = ""): FileList? {
        //https://developers.google.com/drive/v3/web/search-parameters
        val modifiedTime = DateTime(sinceWhen).toStringRfc3339()
        val query = "mimeType='application/vnd.google-apps.spreadsheet' and modifiedTime >= '$modifiedTime'"

        return withContext(Dispatchers.IO) {
            Drive.Builder(httpTransport, jsonFactory, credential()).build()
                .files()
                .list()
                .apply {
                    q = query
                    spaces = "drive"
                    fields = "nextPageToken, files(id, name, modifiedTime)"
                    pageToken = this.pageToken
                }
                .execute()
        }
    }

    suspend fun getSpreadsheetValues(ssID: String): SheetState {
        val subSheets = sheets.spreadsheets().get(ssID).apply { includeGridData = true }.execute().sheets
        return if (subSheets.size == 1) {
            try {
                val rows = fetchSheetValues(sheets, ssID, subSheets.first().properties.title)
                rows.forEach {
                    println("PROW - $it")
                }
                if (rows.isEmpty()) throw IllegalStateException()
                val sheet = rows.toDriveSheet()
                SheetState.SelectedSheet(sheet)
            } catch (e: IllegalStateException) {
                SheetState.InvalidSheet
            }
        } else {
            SheetState.MultipleSheets(ssID, subSheets.map { it.properties.title })
        }
    }

    suspend fun getSpreadsheetValuesFromSubSheet(ssID: String, name: String): SheetState {
        val rows = fetchSheetValues(sheets, ssID, name)
        rows.forEach {
            println("PROW - $it")
        }
        return try {
            SheetState.SelectedSheet(rows.toDriveSheet())
        } catch (e: Exception) {
            SheetState.InvalidSheet
        }
    }

    private suspend fun fetchSheetValues(
        sheets: Sheets,
        ssID: String,
        name: String,
    ) = withContext(Dispatchers.IO) {
        sheets.spreadsheets().values().get(ssID, name).execute()
            .getValues() as List<List<String>>
    }
}

sealed class SheetState {
    class MultipleSheets(val ssID: String, val sheetNames: List<String>) : SheetState()
    class SelectedSheet(val sheet: DriveSheet) : SheetState()
    object InvalidSheet : SheetState()
}

data class DriveSheet(
    val headers: List<String>,
    val rows: List<List<String>>,
    val firstNameIndex: Int = -1,
    val lastNameIndex: Int = -1,
    val cellIndex: Int = -1,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.createStringArrayList() ?: emptyList(),
        parcel.createStringArrayList()?.map { it.split(",") } ?: emptyList(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeStringList(headers)
        parcel.writeStringList(rows.map { it.joinToString(",") })
        parcel.writeInt(firstNameIndex)
        parcel.writeInt(lastNameIndex)
        parcel.writeInt(cellIndex)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DriveSheet> {
        override fun createFromParcel(parcel: Parcel): DriveSheet {
            return DriveSheet(parcel)
        }

        override fun newArray(size: Int): Array<DriveSheet?> {
            return arrayOfNulls(size)
        }
    }
}

fun List<List<String>>.toDriveSheet() = try {
    val header = first()
    var defaultCellIndex = -1
    var defaultFirstNameIndex = -1
    var defaultLastNameIndex = -1
    header.forEachIndexed { index, s ->
        when {
            Regex("(?i)first( name)?|firstName").matches(s) -> defaultFirstNameIndex = index
            Regex("(?i)last( name)?|lastName").matches(s) -> defaultLastNameIndex = index
            Regex("(?i)cell(ular)?|phone(1|\\s)?|mobile").matches(s) -> defaultCellIndex = index
        }
    }
    DriveSheet(
        firstNameIndex = defaultFirstNameIndex,
        lastNameIndex = defaultLastNameIndex,
        cellIndex = defaultCellIndex,
        headers = first(),
        rows = subList(1, size)
    )
} catch (e: Exception) {
    throw SpreadsheetParsingException
}

object SpreadsheetParsingException : Exception()

fun DriveSheet.update(label: ColumnLabel?, index: Int) = when (label) {
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
