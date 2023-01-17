package com.simplemobiletools.smsmessenger.receivers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony.Sms
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.simplemobiletools.boomorganized.BoomOrganizerWorker
import com.simplemobiletools.commons.extensions.getMyContactsCursor
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.smsmessenger.extensions.boomOrganizedDB
import com.simplemobiletools.smsmessenger.extensions.getMessageRecipientAddress
import com.simplemobiletools.smsmessenger.extensions.getNameFromAddress
import com.simplemobiletools.smsmessenger.extensions.getThreadId
import com.simplemobiletools.smsmessenger.extensions.messagesDB
import com.simplemobiletools.smsmessenger.extensions.messagingUtils
import com.simplemobiletools.smsmessenger.extensions.notificationHelper
import com.simplemobiletools.smsmessenger.helpers.refreshMessages
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/** Handles updating databases and states when a SMS message is sent. */
class SmsStatusSentReceiver : SendStatusReceiver() {

    override fun updateAndroidDatabase(context: Context, intent: Intent, receiverResultCode: Int) {
        val messageUri: Uri? = intent.data
        val resultCode = resultCode
        val messagingUtils = context.messagingUtils

        val type = if (resultCode == Activity.RESULT_OK) {
            Sms.MESSAGE_TYPE_SENT
        } else {
            Sms.MESSAGE_TYPE_FAILED
        }
        messagingUtils.updateSmsMessageSendingStatus(messageUri, type)
        messagingUtils.maybeShowErrorToast(
            resultCode = resultCode,
            errorCode = intent.getIntExtra(EXTRA_ERROR_CODE, NO_ERROR_CODE)
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun updateAppDatabase(context: Context, intent: Intent, receiverResultCode: Int) {
        val messageUri = intent.data
        if (messageUri != null) {
            val messageId = messageUri.lastPathSegment?.toLong() ?: 0L
            ensureBackgroundThread {
                val type = if (receiverResultCode == Activity.RESULT_OK) {
                    Sms.MESSAGE_TYPE_SENT
                } else {
                    showSendingFailedNotification(context, messageId)
                    Sms.MESSAGE_TYPE_FAILED
                }
                intent.extras?.getString(BoomOrganizerWorker.BOOM_ORGANIZED_ENTRY)?.let {
                    GlobalScope.launch {
                        context.boomOrganizedDB.updateMessageStatusToSent(it)
                    }
                }
                context.messagesDB.updateType(messageId, type)
                refreshMessages()
            }
        }
    }

    private fun showSendingFailedNotification(context: Context, messageId: Long) {
        Handler(Looper.getMainLooper()).post {
            if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                return@post
            }
            val privateCursor = context.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
            ensureBackgroundThread {
                val address = context.getMessageRecipientAddress(messageId)
                val threadId = context.getThreadId(address)
                val recipientName = context.getNameFromAddress(address, privateCursor)
                context.notificationHelper.showSendingFailedNotification(recipientName, threadId)
            }
        }
    }
}
