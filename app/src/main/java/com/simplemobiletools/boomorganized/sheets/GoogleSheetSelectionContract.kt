package com.simplemobiletools.boomorganized.sheets

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.simplemobiletools.boomorganized.FilterableUserSheet
import com.simplemobiletools.boomorganized.sheets.GoogleSheetSelectionActivity.Companion.GOOGLE_SHEET_RESULT

class GoogleSheetSelectionContract : ActivityResultContract<Unit, FilterableUserSheet?>() {

    override fun createIntent(context: Context, input: Unit): Intent {
        return GoogleSheetSelectionActivity.createIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?) = intent?.getParcelableExtra<FilterableUserSheet?>(GOOGLE_SHEET_RESULT)
}
