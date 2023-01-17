package com.simplemobiletools.smsmessenger.messaging

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.FileProvider
import com.simplemobiletools.smsmessenger.helpers.SCHEDULED_MESSAGE_ID
import com.simplemobiletools.smsmessenger.helpers.THREAD_ID
import com.simplemobiletools.smsmessenger.models.Message
import com.simplemobiletools.smsmessenger.receivers.ScheduledMessageReceiver
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * All things related to scheduled messages are here.
 */

fun Context.getScheduleSendPendingIntent(message: Message): PendingIntent {
    val intent = Intent(this, ScheduledMessageReceiver::class.java)
    intent.putExtra(THREAD_ID, message.threadId)
    intent.putExtra(SCHEDULED_MESSAGE_ID, message.id)

    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    return PendingIntent.getBroadcast(this, message.id.toInt(), intent, flags)
}

fun Context.scheduleMessage(message: Message) {
    val pendingIntent = getScheduleSendPendingIntent(message)
    val triggerAtMillis = message.millis()

    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
}

fun Context.cancelScheduleSendPendingIntent(messageId: Long) {
    val intent = Intent(this, ScheduledMessageReceiver::class.java)
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    PendingIntent.getBroadcast(this, messageId.toInt(), intent, flags).cancel()
}

@SuppressLint("InlinedApi")
fun Context.storeImageForKeepsAndReturnUri(bitmap: Bitmap): Uri {
    val timeStamp =
        SimpleDateFormat("yyyyMMdd_Hms", Locale.getDefault()).format(Date())
    val fileName = "temp_$timeStamp.png"

    @SuppressLint("InlinedApi")
    fun saveImageInQ(bitmap: Bitmap): Uri {
        val fos: OutputStream?
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        //use application context to get contentResolver
        val contentResolver = applicationContext.contentResolver
        val uri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let { contentResolver.openOutputStream(it) }.also { fos = it }
        fos?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        fos?.flush()
        fos?.close()

        contentValues.clear()
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
        uri?.let {
            contentResolver.update(it, contentValues, null, null)
        }
        return uri!!
    }

    fun legacySave(bitmap: Bitmap): Uri {
        val appContext = applicationContext
        val directory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val file = File(directory, fileName)
        val outStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        outStream.flush()
        outStream.close()
        MediaScannerConnection.scanFile(
            appContext, arrayOf(file.absolutePath),
            null, null
        )
        return file.safelyToUri(appContext)
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) saveImageInQ(bitmap)
    else legacySave(bitmap)
}

fun File.safelyToUri(context: Context): Uri =
    FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider", this
    )