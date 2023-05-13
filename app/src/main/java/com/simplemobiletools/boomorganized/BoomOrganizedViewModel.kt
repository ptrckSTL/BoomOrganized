@file:OptIn(FlowPreview::class)

package com.simplemobiletools.boomorganized

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.simplemobiletools.boomorganized.BoomOrganizedWorkRepo.workState
import com.simplemobiletools.boomorganized.BoomOrganizerWorker.Companion.WORK_TAG
import com.simplemobiletools.smsmessenger.App
import com.simplemobiletools.smsmessenger.interfaces.BoomStatus
import com.simplemobiletools.smsmessenger.interfaces.OrganizedContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private var latestCsvState: CsvState = CsvState.None

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

    fun onClearAttachment() {
        imageUri = null
        _state.update {
            when (it) {
                is BoomOrganizedViewState.CsvAndPreview -> {
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
        viewModelScope.launch {
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
            val userCsv = withContext(coroutineContext) {
                contentResolver.openInputStream(uri).use {
                    if (it == null) return@withContext CsvState.Error("Couldn't open the input stream")
                    with(reader.readAll(it)) {
                        val headers = firstOrNull()
                        if (headers == null || this.size < 2) { // need to have at least one row of non-header content
                            CsvState.Error("Malformed or empty CSV was found, please select a different file")
                        } else {
                            val firstNameCol = findStringValueColumnNumberOrNull(headers, *firstNameTemplates)
                            val lastNameCol = findStringValueColumnNumberOrNull(headers, *lastNameTemplates)
                            val cellCol = findStringValueColumnNumberOrNull(headers, *cellTemplates)
                            if (cellCol == null) CsvState.Error("Need a cell phone column")
                            else CsvState.Found(this, firstNameCol, lastNameCol, cellCol)
                        }
                    }
                }
            }
            latestCsvState = userCsv
            _state.value = when (userCsv) {
                is CsvState.Found -> BoomOrganizedViewState.CsvAndPreview(
                    csvState = userCsv,
                    preview = replaceTemplates(script, userCsv.firstName(1), userCsv.lastName(1)),
                    photoUri = imageUri
                )

                is CsvState.Error -> BoomOrganizedViewState.CsvAndPreview(
                    csvState = userCsv,
                    preview = userCsv.msg,
                    imageUri
                )

                CsvState.None -> BoomOrganizedViewState.CsvAndPreview(
                    csvState = userCsv,
                    preview = "Somehow no state",
                    photoUri = imageUri
                )
            }
        }
    }

    private fun findStringValueColumnNumberOrNull(list: List<String>, vararg values: String): Int? {
        list.forEachIndexed { index, s ->
            if (s in values) return index
        }
        return null
    }

    fun loadNextViewState() {
        viewModelScope.launch {
            when (state.value) {
                is BoomOrganizedViewState.RapAndImage -> _state.value = BoomOrganizedViewState.CsvAndPreview(latestCsvState, script, imageUri)
                is BoomOrganizedViewState.CsvAndPreview -> {
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
        when (val state = latestCsvState) {
            is CsvState.Error -> _state.update { BoomOrganizedViewState.CsvAndPreview(state, "CSV was malformed, use something else.", imageUri) }
            is CsvState.None -> Unit
            is CsvState.Found -> {
                with(state) {
                    clearDB()
                    for (index in (1..csv.lastIndex)) { // skip head
                        val cell = csv[index][cellCol]
                        // TODO check for empty names
                        val firstName = firstNameCol?.let { csv[index][it] }
                        val lastName = lastNameCol?.let { csv[index][it] }
                        if (Patterns.PHONE.matcher(cell).matches())
                            upsertContact(
                                OrganizedContact(
                                    cell = csv[index][cellCol],
                                    firstName = firstName,
                                    lastName = lastName,
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
            is BoomOrganizedViewState.CsvAndPreview -> {
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

sealed class CsvState {
    object None : CsvState()
    class Error(val msg: String) : CsvState() {
        override fun equals(other: Any?) = other is Error && msg == other.msg
        override fun hashCode() = msg.hashCode()
    }

    data class Found(val csv: Csv, val firstNameCol: Int?, val lastNameCol: Int?, val cellCol: Int) : CsvState() {
        override fun equals(other: Any?) = other is Found && csv == other.csv && firstNameCol == other.firstNameCol && cellCol == other.cellCol
        override fun hashCode(): Int {
            var result = firstNameCol ?: 0
            result = 31 * result + (lastNameCol ?: 0)
            result = 31 * result + cellCol
            return result
        }
    }
}

fun CsvState.Found.firstName(index: Int) = if (firstNameCol != null) csv[index][firstNameCol] else null
fun CsvState.Found.lastName(index: Int) = if (lastNameCol != null) csv[index][lastNameCol] else null
fun CsvState.Found.cell(index: Int) = csv[index][cellCol]

val CsvState.rows: Int?
    get() = (this as? CsvState.Found?)?.csv?.size?.minus(1) // don't count header

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

    data class CsvAndPreview(
        val csvState: CsvState,
        val preview: String,
        val photoUri: Uri?,
    ) : BoomOrganizedViewState() {
        override fun equals(other: Any?) = other is CsvAndPreview && csvState == other.csvState
        override fun hashCode(): Int {
            var result = csvState.hashCode()
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

typealias Csv = List<List<String>>

val firstNameTemplates = arrayOf("firstName", "first_name", "first name", "first")
val lastNameTemplates = arrayOf("lastName", "last_name", "last name", "last")
val cellTemplates = arrayOf("cell", "phone", "cell_phone", "cellphone")

data class Contact(
    val rap: String,
    val cellNumber: String,
    val firstName: String,
    val lastName: String,
)