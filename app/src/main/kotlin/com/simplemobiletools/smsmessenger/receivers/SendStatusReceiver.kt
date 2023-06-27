package com.simplemobiletools.smsmessenger.receivers

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.boomorganized.BoomOrganizerWorker
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.smsmessenger.extensions.boomOrganizedDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

abstract class SendStatusReceiver : BroadcastReceiver() {
    // Updates the status of the message in the internal database
    abstract fun updateAndroidDatabase(context: Context, intent: Intent, receiverResultCode: Int)

    // allows the implementer to update the status of the message in their database
    abstract fun updateAppDatabase(context: Context, intent: Intent, receiverResultCode: Int)

    override fun onReceive(context: Context, intent: Intent) {
        val resultCode = resultCode
        ensureBackgroundThread {
            updateAndroidDatabase(context, intent, resultCode)
            updateAppDatabase(context, intent, resultCode)
        }
        if (resultCode == Activity.RESULT_OK) {
            intent.extras?.getString(BoomOrganizerWorker.BOOM_ORGANIZED_ENTRY)?.let {
                MainScope().launch(Dispatchers.IO) {
                    context.boomOrganizedDB.updateMessageStatusToSent(it)
                }
            }
        }
    }

    companion object {
        const val SMS_SENT_ACTION = "com.simplemobiletools.smsmessenger.receiver.SMS_SENT"
        const val SMS_DELIVERED_ACTION = "com.simplemobiletools.smsmessenger.receiver.SMS_DELIVERED"

        // Defined by platform, but no constant provided. See docs for SmsManager.sendTextMessage.
        const val EXTRA_ERROR_CODE = "errorCode"
        const val EXTRA_SUB_ID = "subId"

        const val NO_ERROR_CODE = -1
    }
}
