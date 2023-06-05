@file:OptIn(FlowPreview::class)

package com.simplemobiletools.boomorganized

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.simplemobiletools.boomorganized.BoomOrganizedWorkRepo.workState
import com.simplemobiletools.boomorganized.BoomOrganizerWorker.Companion.WORK_TAG
import com.simplemobiletools.boomorganized.sheets.ColumnLabel
import com.simplemobiletools.boomorganized.sheets.SheetError
import com.simplemobiletools.boomorganized.sheets.UserSheet
import com.simplemobiletools.smsmessenger.App
import com.simplemobiletools.smsmessenger.interfaces.BoomStatus
import com.simplemobiletools.smsmessenger.interfaces.OrganizedContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak", "MissingPermission")
class BoomOrganizedViewModel : ViewModel(), BoomOrganizedPrefs, OrganizedContactsRepo {
    override val boomContext: Context = App.instance

    private val _state: MutableStateFlow<BoomOrganizedViewState> = MutableStateFlow(BoomOrganizedViewState.Uninitiated)
    val state = combineStates(_state, workState) { viewState, combinedWorkState ->
        val (workState, counts) = combinedWorkState
        if (workState is BoomOrganizedWorkState.Complete) {
            BoomOrganizedViewState.OrganizationComplete(workState.counts)
        } else {
            (viewState as? BoomOrganizedViewState.BoomOrganizedExecute)?.let {
                when (workState) {
                    is BoomOrganizedWorkState.Executing -> it.copy(
                        contact = workState.currentContact,
                        counts = counts,
                        isPaused = false,
                        isLoading = false
                    )

                    is BoomOrganizedWorkState.Loading -> it.copy(
                        isLoading = true,
                        isPaused = false
                    )

                    is BoomOrganizedWorkState.Paused -> it.copy(
                        counts = counts,
                        isPaused = true,
                        isLoading = false
                    )

                    else -> it
                }
            } ?: viewState
        }
    }

    private var latestUserSheetState: UserSheetState = UserSheetState.None

    init {
        generateInitialState()
    }

    fun updateAttachmentState(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            imageUri = uri
            _state.update {
                when (it) {
                    is BoomOrganizedViewState.OfferToResume -> it.copy(photoUri = uri)
                    else -> BoomOrganizedViewState.RapAndImage(script, uri)

                }
            }
        }
    }

    fun handleGoogleSheetResult(sheet: UserSheet?) {
        sheet?.let {
            setCsvState(
                UserSheetState.Found(
                    it.toFilterableDriveSheet()
                )
            )
        }
    }

    fun handleCsvUpload(sheet: List<List<String>>) {
        _state.value = BoomOrganizedViewState.RequestLabels(
            requiredLabels = listOf(),
            sheet = FilterableUserSheet.fromRows(sheet),
            error = SheetError.None
        )
    }

    fun onClearAttachment() {
        imageUri = null
        _state.update {
            when (it) {
                is BoomOrganizedViewState.PreviewOutgoing -> {
                    it.copy(photoUri = null)
                }

                is BoomOrganizedViewState.OfferToResume -> {
                    it.copy(photoUri = null)
                }

                is BoomOrganizedViewState.RapAndImage -> {
                    it.copy(photoUri = null)
                }

                else -> it
            }
        }
    }

    private fun generateInitialState() {
        viewModelScope.launch(Dispatchers.IO) {
            // Are we in the "That's Organizing, Baby" state? Then reset the WorkRepo
            if (state.value is BoomOrganizedViewState.OrganizationComplete &&
                WorkManager.getInstance(boomContext)
                    .getWorkInfosByTag(WORK_TAG)
                    .await()
                    .first()
                    .state
                    .isFinished
            ) {
                // this is the only place where we actually reset the WorkRepo state
                BoomOrganizedWorkRepo.reset()
            }
            val work = workState.value.first
            if (work is BoomOrganizedWorkState.Executing) {
                _state.value = BoomOrganizedViewState.BoomOrganizedExecute("", ContactCounts(), isPaused = false, isLoading = true)
            }
            val contacts = dao.getPendingContacts()
            val contactCount = contacts.toContactCount()
            if (contactCount.pending > 0) {
                offerToResumeWithPendingContacts(
                    contactCount,
                    contacts.first().firstName ?: "",
                    contacts.first().lastName ?: ""
                )
            } else {
                _state.value = BoomOrganizedViewState.RapAndImage(script, imageUri)
            }
        }
    }

    fun takeCsv(uri: Uri, contentResolver: ContentResolver) {
        val reader = CsvReader()
        viewModelScope.launch(Dispatchers.IO) {

            contentResolver.openInputStream(uri).use {
                if (it == null) setCsvState(UserSheetState.Error("Couldn't open the input stream"))
                else {
                    with(reader.readAll(it)) {
                        val headers = firstOrNull()
                        if (headers == null || this.size < 2) { // need to have at least one row of non-header content
                            UserSheetState.Error("Malformed or empty CSV was found, please select a different file")
                        } else {
                            handleGoogleSheetResult(UserSheet(this))
                        }
                    }
                }
            }
        }
    }

    fun setCsvState(userCsv: UserSheetState) {
        latestUserSheetState = userCsv
        _state.value = when (userCsv) {
            is UserSheetState.Found -> BoomOrganizedViewState.PreviewOutgoing(
                userSheetState = userCsv,
                preview = "", // TODO
                photoUri = imageUri
            )

            is UserSheetState.Error -> BoomOrganizedViewState.PreviewOutgoing(
                userSheetState = userCsv,
                preview = userCsv.msg,
                imageUri
            )

            UserSheetState.None -> BoomOrganizedViewState.PreviewOutgoing(
                userSheetState = userCsv,
                preview = "Somehow no state",
                photoUri = imageUri
            )
        }
    }

    fun loadNextViewState() {
        viewModelScope.launch(Dispatchers.IO) {
            when (state.value) {
                is BoomOrganizedViewState.RapAndImage -> _state.value = BoomOrganizedViewState.PreviewOutgoing(latestUserSheetState, script, imageUri)
                is BoomOrganizedViewState.PreviewOutgoing -> {
                    populateDatabaseWithContacts()
                    resumeSession()
                }

                is BoomOrganizedViewState.OrganizationComplete -> generateInitialState()
                is BoomOrganizedViewState.OfferToResume -> resumeSession()
                else -> {}
            }
        }
    }

    fun resumeSession() {
        viewModelScope.launch(Dispatchers.IO) {
            sendMessagesToPendingAndUpdateView()
        }
        _state.value = BoomOrganizedViewState.BoomOrganizedExecute("", ContactCounts(), isPaused = false, isLoading = true)
    }

    private suspend fun populateDatabaseWithContacts() {
        when (val state = latestUserSheetState) {
            is UserSheetState.Error -> _state.update { BoomOrganizedViewState.PreviewOutgoing(state, "CSV was malformed, use something else.", imageUri) }
            is UserSheetState.None -> Unit
            is UserSheetState.Found -> {
                with(state) {
                    clearDB()
                    this.filterableUserSheet.rows.forEach { row ->
                        upsertContact(
                            OrganizedContact(
                                cell = row[filterableUserSheet.cellIndex].value,
                                firstName = row[filterableUserSheet.firstNameIndex].value,
                                lastName = row[filterableUserSheet.lastNameIndex].value,
                                status = BoomStatus.PENDING
                            )
                        )
                    }
                }
            }
        }
    }

    private fun sendMessagesToPendingAndUpdateView() {
        val workRequest = OneTimeWorkRequestBuilder<BoomOrganizerWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(
                workDataOf(
                    BoomOrganizerWorker.ATTACHMENT to (imageUri?.toString() ?: ""),
                    BoomOrganizerWorker.SCRIPT to script
                )
            )
            .addTag(WORK_TAG)
            .build()
        viewModelScope.launch(Dispatchers.Main) {
            val workManager = WorkManager.getInstance(boomContext)
            workManager.enqueueUniqueWork("boom_organizer", ExistingWorkPolicy.REPLACE, workRequest)
        }
        _state.value = BoomOrganizedViewState.BoomOrganizedExecute("", ContactCounts(), isPaused = false, isLoading = true)
    }

    fun pauseOrganizing() {
        WorkManager.getInstance(boomContext).cancelAllWorkByTag(WORK_TAG)
        BoomOrganizedWorkRepo.setPaused()
    }

    fun freshStart() {
        viewModelScope.launch(Dispatchers.IO) {
            imageUri = null
            clearDB()
            _state.value = BoomOrganizedViewState.RapAndImage(script, imageUri)
        }
    }

    private fun offerToResumeWithPendingContacts(contactCounts: ContactCounts, firstName: String, lastName: String) {
        _state.value = BoomOrganizedViewState.OfferToResume(
            imageUri,
            replaceTemplates(script, firstName, lastName),
            contactCounts
        )
    }

    fun onBackViewState(systemBackPress: () -> Unit) {
        when (state.value) {
            is BoomOrganizedViewState.PreviewOutgoing -> {
                _state.value = BoomOrganizedViewState.RapAndImage(script, imageUri)
            }

            is BoomOrganizedViewState.BoomOrganizedExecute -> {
                if (workState.value.first is BoomOrganizedWorkState.Executing) systemBackPress()
                else generateInitialState()
            }

            else -> systemBackPress()
        }
    }

    companion object {
        private const val TAG = "BoomOrganizedViewModel"
    }
}

sealed class UserSheetState {
    object None : UserSheetState()
    class Error(val msg: String) : UserSheetState() {
        override fun equals(other: Any?) = other is Error && msg == other.msg
        override fun hashCode() = msg.hashCode()
    }

    data class Found(val filterableUserSheet: FilterableUserSheet) : UserSheetState()
}

sealed class BoomOrganizedViewState {
    data class RapAndImage(
        val script: String,
        val photoUri: Uri?,
    ) : BoomOrganizedViewState() {
        override fun equals(other: Any?) = other is RapAndImage && script == other.script && photoUri == other.photoUri
        override fun hashCode(): Int {
            var result = script.hashCode()
            result = 31 * result + photoUri.hashCode()
            return result
        }
    }

    object SelectSource : BoomOrganizedViewState()
    data class RequestLabels(
        val requiredLabels: List<ColumnLabel>,
        val sheet: FilterableUserSheet,
        val error: SheetError,
    ) : BoomOrganizedViewState()

    data class PreviewOutgoing(
        val userSheetState: UserSheetState,
        val preview: String,
        val photoUri: Uri?,
    ) : BoomOrganizedViewState() {
        override fun equals(other: Any?) = other is PreviewOutgoing && userSheetState == other.userSheetState
        override fun hashCode(): Int {
            var result = userSheetState.hashCode()
            result = 31 * result + preview.hashCode()
            return result
        }
    }

    data class OfferToResume(
        val photoUri: Uri?,
        val preview: String,
        val contactCounts: ContactCounts,
    ) : BoomOrganizedViewState()

    data class BoomOrganizedExecute(
        val contact: String,
        val counts: ContactCounts,
        val isPaused: Boolean,
        val isLoading: Boolean,
    ) :
        BoomOrganizedViewState() {
        override fun equals(other: Any?) = false
        override fun hashCode() = contact.hashCode()
    }

    object Uninitiated : BoomOrganizedViewState()

    class OrganizationComplete(val counts: ContactCounts) : BoomOrganizedViewState() {
        override fun equals(other: Any?) = other is OrganizationComplete && counts == other.counts
        override fun hashCode() = counts.hashCode()
    }
}

val firstNameTemplates = arrayOf("firstName", "first_name", "first name", "first")
val lastNameTemplates = arrayOf("lastName", "last_name", "last name", "last")
val cellTemplates = arrayOf("cell", "phone", "cell_phone", "cellphone")

data class Contact(
    val rap: String,
    val cellNumber: String,
    val firstName: String,
    val lastName: String,
)

