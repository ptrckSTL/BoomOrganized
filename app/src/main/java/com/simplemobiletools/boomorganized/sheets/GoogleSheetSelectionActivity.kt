package com.simplemobiletools.boomorganized.sheets

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.simplemobiletools.boomorganized.getResultFromActivity
import com.simplemobiletools.boomorganized.onComplete
import com.simplemobiletools.boomorganized.ui.theme.BoomOrganizedTheme
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GoogleSheetSelectionActivity : ComponentActivity() {
    private val viewModel: SheetSelectViewModel by viewModels()
    private val exceptionHandler by lazy {
        CoroutineExceptionHandler { _, throwable ->
            if (throwable is UserRecoverableAuthIOException) {
                handleRecoverableAuthException(throwable.intent)
            } else throw throwable
        }
    }

    private fun handleRecoverableAuthException(intent: Intent) {
        signInCallback.launch(intent)
    }

    private val signInCallback: ActivityResultLauncher<Intent> = getResultFromActivity { intent ->
        resultCode.onComplete(
            onFailure = {
                setResult(GOOGLE_SIGN_IN_FAILURE)
                finish()
            },
            onSuccess = {
                lifecycleScope.launch {
                    val account = GoogleSignIn.getLastSignedInAccount(this@GoogleSheetSelectionActivity)

                    DriveRepo.selectedAccount = account!!.account
                    lifecycleScope.launch(Dispatchers.IO) {
                        viewModel.getRecentSheets(exceptionHandler)
                    }
                }

            })
    }

    private fun initiateGoogleSignIn() {
        lifecycleScope.launch(Dispatchers.Main + exceptionHandler) {
            val cachedSignIn = withContext(Dispatchers.IO) { GoogleSignIn.getLastSignedInAccount(this@GoogleSheetSelectionActivity) }
            if (cachedSignIn?.account == null || cachedSignIn.isExpired) {
                requestSignIn()
            } else {
                DriveRepo.selectedAccount = cachedSignIn.account
                viewModel.getRecentSheets(exceptionHandler)
            }
        }
    }

    private fun requestSignIn() {
        signInCallback.launch(oauthClient().signInIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initiateGoogleSignIn()
        setContent {
            BoomOrganizedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val viewState = viewModel.viewState.collectAsState()
                    val state = viewState.value
                    if (state is SheetSelectViewState.Complete) {
                        setResult(RESULT_OK, Intent().apply {
                            putExtra(GOOGLE_SHEET_RESULT, state.userSheet)
                        }
                        )
                        finish()
                    }
                    SheetSelectionScreen(
                        viewState = state,
                        onSheetSelected = viewModel::onSheetSelected,
                        onSubSheetSelected = viewModel::onSubSheetSelected,
                        onLabelSelected = viewModel::onUpdateColumnLabel,
                        onPrevious = viewModel::onNavigationBack,
                        onNext = viewModel::onNavigateNext,
                        onAddExclusiveFilter = viewModel::addExcludeFilter,
                        onAddInclusiveFilter = viewModel::addIncludeFilter,
                        onForceClose = {
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                        },
                        onCreateFilter = viewModel::onCreateFilter
                    )
                }
            }
        }
    }

    companion object {
        const val GOOGLE_SHEET_RESULT = "google_sheet_result"
        const val GOOGLE_SIGN_IN_FAILURE = 500

        fun createIntent(context: Context): Intent {
            return Intent(context, GoogleSheetSelectionActivity::class.java)
        }
    }
}
