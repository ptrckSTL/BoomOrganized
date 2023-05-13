package com.simplemobiletools.boomorganized.oauth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.util.Date

class SheetSelectViewModel : ViewModel() {
    private val _viewState = MutableStateFlow<SheetSelectViewState>(SheetSelectViewState.Uninitiated)
    val viewState = _viewState.asStateFlow()

    suspend fun getRecentSheets(exceptionHandler: CoroutineExceptionHandler) {
        viewModelScope.launch(exceptionHandler) {
            _viewState.emit(
                try {
                    DriveRepo.getListOfFiles(Date(System.currentTimeMillis() - Duration.ofDays(7L).toMillis()))
                        ?.files
                        ?.map { SheetListItem.fromDriveFile(it) }?.let {
                            SheetSelectViewState.SheetsFound(it)
                        } ?: throw IllegalStateException("Failed to collect sheets")
                } catch (e: Exception) {
                    SheetSelectViewState.Error(e.message)
                }
            )
        }
    }

    fun onSubSheetSelected(ssID: String, subSheet: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _viewState.emit(
                SheetSelectViewState.SheetSelected(
                    DriveRepo.getSpreadsheetValuesFromSubSheet(ssID, subSheet).sheet
                )
            )
        }
    }

    fun onSheetSelected(sheetListItem: SheetListItem) {
        updateViewStateWithSheetListItem(sheetListItem)
    }

    private fun updateViewStateWithSheetListItem(sheetListItem: SheetListItem) {
        viewModelScope.launch(Dispatchers.IO) {
            _viewState.emit(
                withContext(coroutineContext) {
                    try {
                        when (val sheetState = DriveRepo.getSpreadsheetValues(sheetListItem.id)) {
                            is SheetState.MultipleSheets -> SheetSelectViewState.SubSheetsFound(
                                sheetState.ssID,
                                sheetState.sheetNames
                            )

                            is SheetState.SelectedSheet -> {
                                when {
                                    sheetState.sheet.headers.isEmpty() -> {
                                        SheetSelectViewState.Error("Tried to open a spreadsheet, but no headers were found.")
                                    }

                                    sheetState.sheet.flatRows.size.mod(sheetState.sheet.headers.size) != 0 -> {
                                        SheetSelectViewState.Error(
                                            """Error occurred while parsing spreadsheet data.
                                                        Header row length did not divide evenly into the total number of cells that we found.
                                                        Please check that there isn't a header with an empty column or something."""
                                        )
                                    }

                                    else -> {
                                        SheetSelectViewState.SheetSelected(sheetState.sheet)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        SheetSelectViewState.Error(e.message)
                    }
                }
            )
        }
    }
}

sealed class SheetSelectViewState {
    object Uninitiated : SheetSelectViewState()
    class Error(val msg: String?) : SheetSelectViewState()
    data class SheetsFound(val sheets: List<SheetListItem>) : SheetSelectViewState()
    data class SubSheetsFound(val ssID: String, val sheets: List<String>) : SheetSelectViewState()
    data class SheetSelected(val sheet: DriveSheet) : SheetSelectViewState()
}