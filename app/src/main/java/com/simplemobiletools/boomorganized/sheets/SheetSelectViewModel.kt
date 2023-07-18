package com.simplemobiletools.boomorganized.sheets

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.molecule.RecompositionClock.Immediate
import app.cash.molecule.launchMolecule
import com.simplemobiletools.boomorganized.*
import com.simplemobiletools.smsmessenger.App
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.util.Date

@SuppressLint("StaticFieldLeak")
class SheetSelectViewModel : ViewModel(), BoomOrganizedPrefs {
    override val boomContext: Context = App.instance

    private val loadingState = MutableStateFlow(false)
    private val _viewState = MutableStateFlow<SheetSelectViewState>(SheetSelectViewState.Uninitiated)

    val viewState = viewModelScope.launchMolecule(Immediate) {
        val view by _viewState.collectAsState(SheetSelectViewState.Uninitiated)
        val loading by loadingState.collectAsState()
        if (loading) view.asLoading() else view.asNotLoading()
    }

    private val navigationStack = ArrayDeque<SheetSelectViewState>()

    private fun stopLoadingStackAndEmit(state: SheetSelectViewState) {
        loadingState.update { false }
        navigationStack.addFirst(viewState.value)
        _viewState.value = state
    }

    private fun stopLoadingAndEmit(state: SheetSelectViewState) {
        loadingState.update { false }
        _viewState.value = state
    }

    suspend fun getRecentSheets(exceptionHandler: CoroutineExceptionHandler) {
        viewModelScope.launch(exceptionHandler) {
            try {
                DriveRepo.getListOfFiles(Date(System.currentTimeMillis() - Duration.ofDays(365L).toMillis()))
                    ?.files
                    ?.map(SheetListItem::fromDriveFile)
                    ?.let(SheetSelectViewState::SheetsFound)
                    ?.run(::stopLoadingAndEmit)
            } catch (e: Exception) {
                exceptionHandler.handleException(coroutineContext, e)
            }
        }
    }

    /**
     * updates current viewstate to loading if it implements Loadable. Does not edit the nav stack.
     */
    private fun emitLoadingState() {
        loadingState.update { true }
    }

    fun onSubSheetSelected(ssID: String, subSheet: String) {
        viewModelScope.launch(Dispatchers.IO) {
            emitLoadingState()
            stopLoadingStackAndEmit(
                when (val state = DriveRepo.getSpreadsheetValuesFromSubSheet(ssID, subSheet)) {
                    is SheetState.SelectedSheet -> SheetSelectViewState.Complete(state.sheet.toFilterableDriveSheet())
                    else -> SheetSelectViewState.Error("Something was wrong about that sheet.")
                }
            )
        }
    }

    fun onSheetSelected(sheetListItem: SheetListItem) {
        emitLoadingState()
        updateViewStateWithSheetListItem(sheetListItem)
    }

    fun onNavigationBack() {
        stopLoadingAndEmit(
            if (navigationStack.isNotEmpty()) {
                navigationStack.removeFirst()
            } else {
                SheetSelectViewState.ForceClose
            }
        )
    }

    private fun updateViewStateWithSheetListItem(sheetListItem: SheetListItem) {
        viewModelScope.launch(Dispatchers.IO) {
            stopLoadingStackAndEmit(
                withContext(coroutineContext) {
                    try {
                        generateViewStateFrom(sheetListItem)
                    } catch (e: Exception) {
                        SheetSelectViewState.Error(e.message)
                    }
                }
            )
        }
    }

    /**
     * If the selected spreadsheet has sub-sheets, allow the user to select one. Otherwise just show the sheet.
     */
    private suspend fun generateViewStateFrom(sheetListItem: SheetListItem) =
        when (val sheetState = DriveRepo.getSpreadsheetValues(sheetListItem.id)) {
            is SheetState.MultipleSheets -> SheetSelectViewState.SubSheetsFound(
                ssID = sheetState.ssID,
                sheets = sheetState.sheetNames,
            )

            is SheetState.SelectedSheet -> {
                when {
                    sheetState.sheet.rows.isEmpty() -> SheetSelectViewState.Error("Tried to open a spreadsheet, but no headers were found.")
                    else -> SheetSelectViewState.Complete(sheetState.sheet.toFilterableDriveSheet())
                }
            }

            SheetState.InvalidSheet -> SheetSelectViewState.Error("Sheet could not be parsed. Maybe it was empty?")
        }
}

sealed class SheetSelectViewState {

    class Complete(val userSheet: FilterableUserSheet) : SheetSelectViewState()
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

}

sealed interface SheetError {
    object None : SheetError
    object NoCellSelected : SheetError
    object NoFirstName : SheetError
    object NoLastName : SheetError
    sealed interface NonCritical : SheetError
    class SomeCellEntriesMissing(val count: Int) : NonCritical {
        override fun equals(other: Any?) = other is SomeCellEntriesMissing && other.count == count
        override fun hashCode() = javaClass.hashCode()
    }
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

fun SheetSelectViewState.asLoading() = (this as? Loadable)?.let {
    when (it) {
        is SheetSelectViewState.SheetsFound -> it.copy(isLoading = true)
        is SheetSelectViewState.SubSheetsFound -> it.copy(isLoading = true)
    }
} ?: this
