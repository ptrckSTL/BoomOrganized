package com.simplemobiletools.boomorganized.oauth

import android.accounts.Account
import com.google.android.gms.common.Scopes
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.drive.Drive
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
        val scopes = listOf(SheetsScopes.SPREADSHEETS, Scopes.DRIVE_FULL, Scopes.DRIVE_FILE)
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

    fun getSpreadsheetValues(ssID: String): SheetState {
        val subSheets = sheets.spreadsheets().get(ssID).apply { includeGridData = true }.execute().sheets
        return if (subSheets.size == 1) {
            println("PATRICK - title = ${subSheets.first().properties.title}")
            val rows = fetchSheetValues(sheets, ssID, subSheets.first().properties.title)
            SheetState.SelectedSheet(DriveSheet.generate(rows))
        } else {
            SheetState.MultipleSheets(ssID, subSheets.map { it.properties.title })
        }
    }

    fun getSpreadsheetValuesFromSubSheet(ssID: String, name: String): SheetState.SelectedSheet {
        println("PATRICK - fetching values by from range: $name")
        val rows = fetchSheetValues(sheets, ssID, name)
        return SheetState.SelectedSheet(DriveSheet.generate(rows))
    }

    private fun fetchSheetValues(
        sheets: Sheets,
        ssID: String,
        name: String,
    ) = sheets.spreadsheets().values().get(ssID, name).execute()
        .getValues() as List<List<String>>
}

sealed class SheetState {
    class MultipleSheets(val ssID: String, val sheetNames: List<String>) : SheetState()
    class SelectedSheet(val sheet: DriveSheet) : SheetState()
}

data class DriveSheet(
    val headers: List<String>,
    val flatRows: List<String>,
) {
    companion object {
        fun generate(values: List<List<String>>): DriveSheet {
            println("PATRICK - headers: ${values.first().size}")
            val flatRows = values.subList(1, values.size).flatten()
            println("PATRICK - rows: ${flatRows}")
            return DriveSheet(values.first(), flatRows)
        }
    }
}

