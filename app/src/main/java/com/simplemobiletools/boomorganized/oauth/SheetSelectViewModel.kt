package com.simplemobiletools.boomorganized.oauth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.util.Date

class SheetSelectViewModel : ViewModel() {
    private val _viewState = MutableStateFlow<SheetSelectViewState>(SheetSelectViewState.Uninitiated)
    val viewState = _viewState.asStateFlow()

    private val navigationStack = ArrayDeque<SheetSelectViewState>()

    private fun stackCurrentEmitNew(state: SheetSelectViewState) {
        navigationStack.add(_viewState.value.asNotLoading())
        _viewState.value = state
        navigationStack.forEachIndexed { index, sheetSelectViewState ->
            println("PATRICK - nav stack: $index, ${sheetSelectViewState.javaClass.simpleName}")
        }
    }

    suspend fun getRecentSheets(exceptionHandler: CoroutineExceptionHandler) {
        viewModelScope.launch(exceptionHandler) {
            try {
                // TODO this should be tunable by the user
                val successState =
                    DriveRepo.getListOfFiles(Date(System.currentTimeMillis() - Duration.ofDays(7L).toMillis()))?.files?.map { SheetListItem.fromDriveFile(it) }
                        ?.let {
                            SheetSelectViewState.SheetsFound(it, false)
                        }

                successState?.let { stackCurrentEmitNew(it) }
            } catch (e: Exception) {
                exceptionHandler.handleException(coroutineContext, e)
            }
        }
    }

    /**
     * updates current viewstate to loading if it implements Loadable. Does not edit the nav stack.
     */
    private fun emitLoadingState() {
        viewModelScope.launch {
            _viewState.update {
                if (it is Loadable) {
                    when (it) {
                        is SheetSelectViewState.SheetsFound -> it.copy(isLoading = true)
                        is SheetSelectViewState.SubSheetsFound -> it.copy(isLoading = true)
                    }
                } else it
            }
        }
    }

    fun onSubSheetSelected(ssID: String, subSheet: String) {
        viewModelScope.launch(Dispatchers.IO) {
            emitLoadingState()
            stackCurrentEmitNew(
                when (val state = DriveRepo.getSpreadsheetValuesFromSubSheet(ssID, subSheet)) {
                    is SheetState.SelectedSheet -> SheetSelectViewState.SheetSelected(state.sheet, emptyList())
                    else -> SheetSelectViewState.Error("Something was wrong about that sheet.")
                }
            )
        }
    }

    fun onSheetSelected(sheetListItem: SheetListItem) {
        emitLoadingState()
        updateViewStateWithSheetListItem(sheetListItem)
    }

    fun onUpdateColumnLabel(index: Int, newLabel: ColumnLabel?) {
        when (val state = _viewState.value) {
            is SheetSelectViewState.SheetSelected -> {
                _viewState.value =
                    SheetSelectViewState.SheetSelected(
                        sheet = state.sheet.update(newLabel, index),
                        userFilter = state.userFilter
                    )
            }

            else -> {
                throw IllegalStateException()
            }
        }
    }

    fun onNavigateNext() {
        when (val state = _viewState.value) {
            is SheetSelectViewState.SheetSelected -> {
                generateNextNavState(state)
            }

            else -> throw IllegalStateException()
        }
    }

    fun onNavigationBack() {
        _viewState.value = if (navigationStack.isNotEmpty()) {
            navigationStack.removeLast()
        } else {
            SheetSelectViewState.ForceClose
        }
    }

    private fun generateNextNavState(select: SheetSelectViewState.SheetSelected) {
        println("PATRICK - error = ${select.error.javaClass.simpleName}")
        when (select.error) {
            // These are warnings, we can move on
            SheetError.NoFirstName, SheetError.NoLastName, is SheetError.SomeCellEntriesMissing -> {
                _viewState.value = SheetSelectViewState.Complete.sanitizeAndConstruct(select.sheet)
            }

            SheetError.None, SheetError.NoCellSelected -> {
                println("PATRICK - ${select.sheet.cellIndex}")
                val sh = select.sheet
                when {
                    sh.firstNameIndex == -1 -> _viewState.value = select.copy(error = SheetError.NoFirstName)
                    sh.lastNameIndex == -1 -> _viewState.value = select.copy(error = SheetError.NoLastName)
                    sh.cellIndex == -1 -> _viewState.value = select.copy(error = SheetError.NoCellSelected)
                    else -> {
                        val missingNumbers = sh.rows.count { row ->
                            val cell = row.getOrNull(sh.cellIndex)
                            cell.isNullOrBlank() || !Patterns.PHONE.matcher(cell).matches()
                        }
                        if (missingNumbers > 0) {
                            _viewState.value = select.copy(error = SheetError.SomeCellEntriesMissing(missingNumbers))
                        } else {
                            _viewState.value = SheetSelectViewState.Complete.sanitizeAndConstruct(select.sheet)
                        }
                    }
                }
            }
        }
    }

    private fun updateViewStateWithSheetListItem(sheetListItem: SheetListItem) {
        viewModelScope.launch(Dispatchers.IO) {
            stackCurrentEmitNew(withContext(coroutineContext) {
                try {
                    when (val sheetState = DriveRepo.getSpreadsheetValues(sheetListItem.id)) {
                        is SheetState.MultipleSheets -> SheetSelectViewState.SubSheetsFound(
                            sheetState.ssID,
                            sheetState.sheetNames,
                        )

                        is SheetState.SelectedSheet -> {
                            when {
                                sheetState.sheet.headers.isEmpty() -> {
                                    SheetSelectViewState.Error("Tried to open a spreadsheet, but no headers were found.")
                                }

                                sheetState.sheet.rows.any { it.size != sheetState.sheet.headers.size } -> {
                                    SheetSelectViewState.Error(
                                        "Error occurred while parsing spreadsheet data.\n" + "Header row length did not divide evenly into the total number of cells that we found.\n" + "Please check that there isn't a header with an empty column or a column without a header.\n"
                                    )
                                }

                                else -> {
                                    SheetSelectViewState.SheetSelected(sheetState.sheet, emptyList())
                                }
                            }
                        }

                        SheetState.InvalidSheet -> SheetSelectViewState.Error("Sheet could not be parsed. Maybe it was empty?")
                    }
                } catch (e: Exception) {
                    SheetSelectViewState.Error(e.message)
                }
            })
        }
    }
}

sealed class SheetSelectViewState {

    class Complete private constructor(val driveSheet: DriveSheet) : SheetSelectViewState() {
        companion object {
            fun sanitizeAndConstruct(driveSheet: DriveSheet) =
                Complete(
                    driveSheet.copy(
                        rows = driveSheet.rows.filter {
                            val cell = it.getOrNull(driveSheet.cellIndex)
                            !cell.isNullOrBlank() && Patterns.PHONE.matcher(cell!!).matches()
                        }
                    )
                )
        }
    }

    object ForceClose : SheetSelectViewState()
    object Uninitiated : SheetSelectViewState()
    class Error(val msg: String?) : SheetSelectViewState()
    data class SheetsFound(
        val sheets: List<SheetListItem>,
        override val isLoading: Boolean = false,
    ) : SheetSelectViewState(), Loadable

    data class SubSheetsFound(
        val ssID: String,
        val sheets: List<String>,
        override val isLoading: Boolean = false,
    ) : SheetSelectViewState(), Loadable

    data class SheetSelected(
        val sheet: DriveSheet,
        val userFilter: List<UserFilter>,
        val error: SheetError = SheetError.None,
    ) : SheetSelectViewState() {
        override fun equals(other: Any?) = other is SheetSelected && other.sheet == sheet && other.error == error
        override fun hashCode() = javaClass.hashCode()
    }
}

sealed class SheetError {
    object None : SheetError()
    object NoCellSelected : SheetError()
    object NoFirstName : SheetError()
    object NoLastName : SheetError()
    class SomeCellEntriesMissing(val count: Int) : SheetError()
}

sealed class UserFilter {
    class Show(val value: String) : UserFilter()
    class Exclude(val value: String) : UserFilter()
}

enum class ColumnLabel {
    FirstName, LastName, CellPhone
}

/**
 * Useful for showing loading state to user and deflecting certain user input when a long running operation is already in
 * progress-- Google Drive/Sheets operations in particular ≤(👀)≥
 */
sealed interface Loadable {
    val isLoading: Boolean
}

fun SheetSelectViewState.asNotLoading() = (this as? Loadable)?.let {
    when (it) {
        is SheetSelectViewState.SheetsFound -> it.copy(isLoading = false)
        is SheetSelectViewState.SubSheetsFound -> it.copy(isLoading = false)
    }
} ?: this
