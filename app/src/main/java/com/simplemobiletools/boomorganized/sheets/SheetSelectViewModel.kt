package com.simplemobiletools.boomorganized.sheets

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplemobiletools.boomorganized.FilterableUserSheet
import com.simplemobiletools.boomorganized.toFilterableDriveSheet
import com.simplemobiletools.boomorganized.update
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

    private var result: FilterableUserSheet? = null
    private fun stackCurrentEmitNew(state: SheetSelectViewState) {
        navigationStack.add(_viewState.value.asNotLoading())
        _viewState.value = state
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
                    is SheetState.SelectedSheet -> SheetSelectViewState.SheetSelected(state.sheet.toFilterableDriveSheet())
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
                        sheet = state.sheet.update(newLabel, index)
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

    fun onCreateFilter(columnIndex: Int) {
        (_viewState.value as? SheetSelectViewState.SheetSelected)?.let {
            result = it.sheet
            _viewState.value = SheetSelectViewState.FilterSetup(
                it.sheet.headers[columnIndex],
                it.sheet.rows[columnIndex]
            )
        }
    }

    fun addIncludeFilter(index: Int, value: String) {
        (_viewState.value as? SheetSelectViewState.FilterSetup)?.let { state ->
            _viewState.value = state.copy(
                values = state.values.toMutableList().apply {
                    this[index] = FilterableValue.Include(value)
                })
        }
    }

    fun addExcludeFilter(index: Int, value: String) {
        (_viewState.value as? SheetSelectViewState.FilterSetup)?.let { state ->
            _viewState.value = state.copy(
                values = state.values.toMutableList().apply {
                    this[index] = FilterableValue.Exclude(value)
                })
        }
    }

    private fun generateNextNavState(select: SheetSelectViewState.SheetSelected) {
        when (select.error) {
            // These are warnings, we can move on
            SheetError.NoFirstName, SheetError.NoLastName, is SheetError.SomeCellEntriesMissing -> {
                _viewState.value = SheetSelectViewState.Complete.sanitizeAndConstruct(select.sheet)
            }

            SheetError.None, SheetError.NoCellSelected -> {
                val sh = select.sheet
                when {
                    sh.firstNameIndex == -1 -> _viewState.value = select.copy(error = SheetError.NoFirstName)
                    sh.lastNameIndex == -1 -> _viewState.value = select.copy(error = SheetError.NoLastName)
                    sh.cellIndex == -1 -> _viewState.value = select.copy(error = SheetError.NoCellSelected)
                    else -> {
                        val missingNumbers = sh.rows.count { row ->
                            val cell = row.getOrNull(sh.cellIndex)?.value
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
                                sheetState.sheet.rows.isEmpty() -> {
                                    SheetSelectViewState.Error("Tried to open a spreadsheet, but no headers were found.")
                                }

                                else -> {
                                    SheetSelectViewState.SheetSelected(sheetState.sheet.toFilterableDriveSheet())
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

    class Complete private constructor(val userSheet: FilterableUserSheet) : SheetSelectViewState() {
        companion object {
            fun sanitizeAndConstruct(userSheet: FilterableUserSheet) =
                Complete(
                    userSheet.copy(
                        rows = userSheet.rows.filter {
                            val cell = it.getOrNull(userSheet.cellIndex)?.value
                            !cell.isNullOrBlank() && Patterns.PHONE.matcher(cell).matches()
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
        val sheet: FilterableUserSheet,
        val error: SheetError = SheetError.None,
    ) : SheetSelectViewState() {
        override fun equals(other: Any?) = other is SheetSelected && other.sheet == sheet && other.error == error
        override fun hashCode() = javaClass.hashCode()
    }

    data class FilterSetup(
        val header: String,
        val values: List<FilterableValue>
    ) : SheetSelectViewState()
}

sealed class SheetError {
    object None : SheetError()
    object NoCellSelected : SheetError()
    object NoFirstName : SheetError()
    object NoLastName : SheetError()
    class SomeCellEntriesMissing(val count: Int) : SheetError()
}


enum class ColumnLabel {
    FirstName, LastName, CellPhone
}

/**
 * Useful for showing loading state to user and deflecting certain user input when a long running operation is already in
 * progress-- Google Drive/Sheets operations in particular ≤(👀 )≥
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
