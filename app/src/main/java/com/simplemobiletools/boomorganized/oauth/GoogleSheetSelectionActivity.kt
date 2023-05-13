package com.simplemobiletools.boomorganized.oauth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.simplemobiletools.boomorganized.getResultFromActivity
import com.simplemobiletools.boomorganized.onComplete
import com.simplemobiletools.smsmessenger.BuildConfig
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
                println("PATRICK - failure")
                println("PATRICK - ${BuildConfig.APPLICATION_ID}")
                println("PATRICK - ${BuildConfig.BUILD_TYPE}")
            },
            onSuccess = {
                lifecycleScope.launch {
                    withContext(coroutineContext + exceptionHandler) { GoogleSignIn.getSignedInAccountFromIntent(intent) }
                        .addOnSuccessListener(this@GoogleSheetSelectionActivity) { account ->
                            lifecycleScope.launch {
                                viewModel.getRecentSheets(exceptionHandler)
                            }
                            DriveRepo.selectedAccount = account.account
                        }
                        .addOnFailureListener(this@GoogleSheetSelectionActivity) { _ ->
                            println("PATRICK - failed after success")
                        }
                }
                GoogleSignIn.getSignedInAccountFromIntent(data)
            }
        )
    }

    private fun initiateGoogleSignIn() {
        val cachedSignIn = lifecycleScope.async { GoogleSignIn.getLastSignedInAccount(this@GoogleSheetSelectionActivity) }
        lifecycleScope.launch(Dispatchers.Main + exceptionHandler) {
            val signIn = cachedSignIn.await()
            if (signIn == null || signIn.isExpired) {
                println("PATRICK - requesting sign in...")
                requestSignIn()
            } else {
                println("PATRICK - getRecentSheets fired")
                viewModel.getRecentSheets(exceptionHandler)
                DriveRepo.selectedAccount = signIn.account
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
            SheetSelectionScreen(
                viewState = viewModel.viewState.collectAsState().value,
                onSheetSelected = viewModel::onSheetSelected,
                onSubSheetSelected = viewModel::onSubSheetSelected
            )
        }
    }
}