package com.simplemobiletools.boomorganized

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.simplemobiletools.commons.extensions.notificationManager
import com.simplemobiletools.smsmessenger.R
import com.simplemobiletools.smsmessenger.extensions.boomOrganizedDB
import com.simplemobiletools.smsmessenger.extensions.subscriptionManagerCompat
import com.simplemobiletools.smsmessenger.helpers.BOOM_ORGANIZED_NOTIFICATION_CHANNEL
import com.simplemobiletools.smsmessenger.interfaces.BoomStatus
import com.simplemobiletools.smsmessenger.interfaces.OrganizedContact
import com.simplemobiletools.smsmessenger.messaging.sendMessageCompat
import com.simplemobiletools.smsmessenger.models.Attachment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.withContext

@SuppressLint("MissingPermission")
class BoomOrganizerWorker(private val appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters), OrganizedContactsRepo {

    val subscriptionId by lazy {
        appContext
            .subscriptionManagerCompat()
            .activeSubscriptionInfoList
            .firstOrNull()
            ?.subscriptionId ?: SmsManager.getDefaultSmsSubscriptionId()
    }
    private val notificationManager = appContext.notificationManager

    override suspend fun doWork(): Result {
        val contacts = appContext
            .boomOrganizedDB
            .getPendingContacts()
            .filter { it.status == BoomStatus.PENDING }
        val attachmentUri = inputData.getString(ATTACHMENT)
        val attachment = if (attachmentUri.isNullOrEmpty()) emptyList()
        else {
            listOf(
                makeAttachment(attachmentUri.toUri())
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                contacts.asFlow()
                    .onStart {
                        BoomOrganizedWorkRepo.setLoading()
                    }
                    .withIndex()
                    .onEach {
                        delay(2500L) // enforce a delay so we don't crash rapidly canceling/resuming
                        // Update work repo and progress notification bar
                        val contactName = "${it.value.firstName ?: ""} ${it.value.lastName ?: ""}"
                        setStatusToSending(it.value)
                        BoomOrganizedWorkRepo.setExecuting(contactName)

                        setForeground(ForegroundInfo(123, updateNotification(contacts.size, it.index + 1, contactName)))

                        // send the message
                        appContext.sendMessageCompat(
                            replaceTemplates(inputData.getString(SCRIPT)!!, it.value.firstName, it.value.lastName),
                            listOf(it.value.cell),
                            subscriptionId,
                            attachment,
                            it.value.cell + it.value.firstName
                        )
                    }
                    .collect {}
                if (appContext.boomOrganizedDB.getPendingContacts().toContactCount().pending == 0) {
                    BoomOrganizedWorkRepo.setComplete()
                    Result.success()
                } else Result.failure()
            } catch (e: CancellationException) {
                BoomOrganizedWorkRepo.setPaused()
                Result.failure()
            }
        }
    }

    private suspend fun getBitmapWidthAndHeight(uri: Uri) = withContext(Dispatchers.IO) {
        this@BoomOrganizerWorker.appContext.contentResolver.openInputStream(uri).use { data ->
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(data)
            options.outWidth to options.outHeight
        }
    }

    private fun updateNotification(total: Int, current: Int, name: String): Notification {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                BOOM_ORGANIZED_NOTIFICATION_CHANNEL,
                "boom organized progress",
                NotificationManager.IMPORTANCE_LOW
            )
        )

        return NotificationCompat.Builder(appContext, BOOM_ORGANIZED_NOTIFICATION_CHANNEL)
            .setChannelId(BOOM_ORGANIZED_NOTIFICATION_CHANNEL)
            .setContentTitle("Organizing in progress")
            .setContentText("Now organizing $name")
            .setOngoing(true)
            .setSmallIcon(R.drawable.rolling_bomb_svgrepo_com)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setProgress(total, current, false)
            .setSilent(true)
            .addAction(android.R.drawable.ic_delete, "Cancel", WorkManager.getInstance(appContext).createCancelPendingIntent(id))
            .build()
    }

    private suspend fun makeAttachment(uri: Uri): Attachment {
        val (width, height) = getBitmapWidthAndHeight(uri)
        return Attachment(
            id = System.currentTimeMillis(),
            mimetype = appContext.contentResolver.getType(uri) ?: "",
            filename = "attachment",
            messageId = 0,
            uriString = uri.toString(),
            width = width,
            height = height
        )
    }

    companion object {
        const val CONTACT_NAME = "contact_name"
        const val TOTAL_PENDING = "total_pending"
        const val CURRENT_PROGRESS = "current_index"
        const val SCRIPT = "script"
        const val ATTACHMENT = "attachment"
        const val WORK_TAG = "boom_organize_worker_tag"
        const val BOOM_ORGANIZED_ENTRY = "boom_organized_entry"
    }
}

object BoomOrganizedWorkRepo : OrganizedContactsRepo {
    private val contactCounts = MutableStateFlow(ContactCounts())
    private val _workState: MutableStateFlow<BoomOrganizedWorkState> = MutableStateFlow(BoomOrganizedWorkState.Uninitiated)

    val workState = combineStates(_workState, contactCounts) { workState, contacts ->
        workState to contacts
    }

    init {
        dao.observeAllPendingContactsAsFlow()
            .onEach {
                contactCounts.value = dao.getPendingContacts().toContactCount()
            }
            .launchIn(GlobalScope)
    }

    fun setPaused() {
        _workState.value = BoomOrganizedWorkState.Paused
    }

    fun setComplete() {
        _workState.value = BoomOrganizedWorkState.Complete(contactCounts.value)
    }

    fun setExecuting(currentContact: String) {
        _workState.value = BoomOrganizedWorkState.Executing(currentContact)
    }

    fun setLoading() {
        _workState.value = BoomOrganizedWorkState.Loading
    }

    fun reset() {
        _workState.value = BoomOrganizedWorkState.Uninitiated
        contactCounts.value = ContactCounts()
    }
}

sealed class BoomOrganizedWorkState {
    object Uninitiated : BoomOrganizedWorkState()
    object Paused : BoomOrganizedWorkState()

    data class Executing(
        val currentContact: String,
    ) : BoomOrganizedWorkState() {
        override fun equals(other: Any?): Boolean {
            return other is Executing && currentContact == other.currentContact
        }

        override fun hashCode(): Int {
            return currentContact.hashCode()
        }
    }

    object Loading : BoomOrganizedWorkState()
    class Complete(val counts: ContactCounts) : BoomOrganizedWorkState() {
        override fun equals(other: Any?) = other is Complete && counts == other.counts
        override fun hashCode() = counts.hashCode()
    }
}

data class ContactCounts(val currentContact: String = "", val pending: Int = 0, val sending: Int = 0, val sent: Int = 0) {
    override fun equals(other: Any?) =
        other is ContactCounts && pending == other.pending && sending == other.sending && sent == other.sent && currentContact == other.currentContact

    override fun hashCode(): Int {
        var result = currentContact.hashCode()
        result = 31 * result + pending
        result = 31 * result + sending
        result = 31 * result + sent
        return result
    }
}

fun ContactCounts.isEmpty() = pending == 0 && sending == 0 && sent == 0

fun List<OrganizedContact>.toContactCount() =
    groupingBy { it.status }
        .eachCount()
        .run {
            ContactCounts(
                pending = this[BoomStatus.PENDING] ?: 0,
                sending = this[BoomStatus.SENDING] ?: 0,
                sent = this[BoomStatus.SENT] ?: 0
            )
        }