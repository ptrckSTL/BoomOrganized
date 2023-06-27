package com.simplemobiletools.boomorganized

import android.app.Activity
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.simplemobiletools.boomorganized.sheets.ColumnLabel
import com.simplemobiletools.boomorganized.sheets.SheetError
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun replaceTemplates(rapWithTemplates: String, firstName: String?, lastName: String?): String {
    var result = rapWithTemplates
    if (firstName != null) {
        result = replaceTemplate(firstNameTemplates, result, firstName)
    }
    if (lastName != null) {
        result = replaceTemplate(lastNameTemplates, result, lastName)
    }
    return result
}

private fun replaceTemplate(templates: Array<String>, rap: String, replacement: String): String {
    for (temp in templates) {
        val replace = rap.replace(temp, replacement)
        if (replace != rap) {
            return replace
        }
    }
    return rap
}


/**
 * Does not produce the same value in a raw, so respect "distinct until changed emissions"
 * */
class DerivedStateFlow<T>(
    private val getValue: () -> T, private val flow: Flow<T>
) : StateFlow<T> {

    override val replayCache: List<T>
        get() = listOf(value)

    override val value: T
        get() = getValue()

    @InternalCoroutinesApi
    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        coroutineScope { flow.distinctUntilChanged().stateIn(this).collect(collector) }
    }
}

fun <T1, R> StateFlow<T1>.mapState(transform: (a: T1) -> R): StateFlow<R> {
    return DerivedStateFlow(getValue = { transform(this.value) }, flow = this.map { a -> transform(a) })
}

fun <T1, T2, R> combineStates(flow: StateFlow<T1>, flow2: StateFlow<T2>, transform: (a: T1, b: T2) -> R): StateFlow<R> {
    return DerivedStateFlow(getValue = { transform(flow.value, flow2.value) }, flow = combine(flow, flow2) { a, b -> transform(a, b) })
}

fun ComponentActivity.getResultFromActivity(callback: ActivityResult.(Intent?) -> Unit) =
    this@getResultFromActivity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        result.callback(result.data)
    }

fun GoogleSignInOptions.Builder.requestVariousScopes(scopes: List<Scope>): GoogleSignInOptions.Builder {
    return if (scopes.size > 1) {
        this.requestScopes(scopes[0], *scopes.subList(1, scopes.lastIndex).toTypedArray())
    } else requestScopes(scopes[0])
}

context(ComponentActivity, ActivityResult)
fun <T> Int.onComplete(onFailure: (Boolean) -> Unit, onSuccess: () -> T) {
    if (this == Activity.RESULT_OK) onSuccess() else onFailure(this == Activity.RESULT_CANCELED)
}

fun Long.formatDateTime(): String {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.US).withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(this))
}

inline fun <reified T : Parcelable> Parcel.createListOfLists(creator: Parcelable.Creator<T>): List<List<T>>? {
    val size = readInt()
    if (size < 0) return null
    val lists = ArrayList<List<T>>(size)
    repeat(size) {
        lists.add(createTypedArrayList(creator) ?: emptyList())
    }
    return lists
}

fun <T : Parcelable> Parcel.writeListOfLists(lists: List<List<T>>) {
    writeInt(lists.size)
    for (list in lists) {
        writeTypedList(list)
    }
}

fun generateLabelErrorState(
    sheet: FilterableUserSheet,
    requiredLabels: HashSet<ColumnLabel>
): SheetError {
    return when {
        requiredLabels.contains(ColumnLabel.FirstName) && sheet.firstNameIndex == -1 -> SheetError.NoFirstName
        requiredLabels.contains(ColumnLabel.LastName) && sheet.lastNameIndex == -1 -> SheetError.NoLastName
        requiredLabels.contains(ColumnLabel.CellPhone) && sheet.cellIndex == -1 -> SheetError.NoCellSelected
        else -> {
            val missingNumbers = sheet.rows.count { row ->
                val cell = row.getOrNull(sheet.cellIndex)
                cell.isNullOrBlank() || !Patterns.PHONE.matcher(cell).matches()
            }
            if (missingNumbers > 0) {
                println("PATRICK found $missingNumbers missing nos")
                SheetError.SomeCellEntriesMissing(missingNumbers)
            }
            SheetError.None
        }
    }
}

fun detectRequiredLabels(script: String): HashSet<ColumnLabel> {
    fun HashSet<ColumnLabel>.addIfContains(columnLabel: ColumnLabel, value: String) {
        if (value in script) add(columnLabel)
    }
    return HashSet<ColumnLabel>().apply {
        add(ColumnLabel.CellPhone)
        addIfContains(ColumnLabel.LastName, "lastName")
        addIfContains(ColumnLabel.FirstName, "firstName")
    }
}

fun generatePreviewScript(script: String, labeledSheet: FilterableUserSheet, requiredLabels: HashSet<ColumnLabel>): String {
    // last chance to throw up if requirements haven't been met for some reason
    val firstName =
        labeledSheet.rows.first().getOrElse(labeledSheet.firstNameIndex) { if (ColumnLabel.FirstName in requiredLabels) throw IllegalStateException() else "" }
    val lastName =
        labeledSheet.rows.first().getOrElse(labeledSheet.firstNameIndex) { if (ColumnLabel.LastName in requiredLabels) throw IllegalStateException() else "" }
    val cell =
        labeledSheet.rows.first().getOrElse(labeledSheet.firstNameIndex) { if (ColumnLabel.CellPhone in requiredLabels) throw IllegalStateException() else "" }

    return script.replace("firstName", firstName).replace("lastName", lastName).replace("cell", cell)
}


fun List<List<String>>.filterBrokenNumbers(index: Int) = filter {
    val cell = it.getOrNull(index)
    !cell.isNullOrBlank() && Patterns.PHONE.matcher(cell).matches()
}
