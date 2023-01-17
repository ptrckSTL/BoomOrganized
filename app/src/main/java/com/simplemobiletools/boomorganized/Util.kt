package com.simplemobiletools.boomorganized

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

fun Long.asHumanReadableTime(): String {
    val seconds = this / 1000
    val hours = seconds / 3600
    val minutes = (seconds - (hours * 3600)) / 60
    val secs = seconds % 60
    return "$hours:$minutes:$secs"
}


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
    private val getValue: () -> T,
    private val flow: Flow<T>
) : StateFlow<T> {

    override val replayCache: List<T>
        get () = listOf(value)

    override val value: T
        get () = getValue()

    @InternalCoroutinesApi
    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        coroutineScope { flow.distinctUntilChanged().stateIn(this).collect(collector) }
    }
}

fun <T1, R> StateFlow<T1>.mapState(transform: (a: T1) -> R): StateFlow<R> {
    return DerivedStateFlow(
        getValue = { transform(this.value) },
        flow = this.map { a -> transform(a) }
    )
}

fun <T1, T2, R> combineStates(flow: StateFlow<T1>, flow2: StateFlow<T2>, transform: (a: T1, b: T2) -> R): StateFlow<R> {
    return DerivedStateFlow(
        getValue = { transform(flow.value, flow2.value) },
        flow = combine(flow, flow2) { a, b -> transform(a, b) }
    )
}
