package com.simplemobiletools.boomorganized.oauth

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.simplemobiletools.boomorganized.oauth.GoogleSheetSelectionActivity.Companion.GOOGLE_SHEET_RESULT

class GoogleSheetSelectionContract : ActivityResultContract<Unit, DriveSheet?>() {

    override fun createIntent(context: Context, input: Unit): Intent {
        return GoogleSheetSelectionActivity.createIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?) = intent?.getParcelableExtra<DriveSheet?>(GOOGLE_SHEET_RESULT)
}
