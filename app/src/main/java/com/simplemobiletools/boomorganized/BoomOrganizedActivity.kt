package com.simplemobiletools.boomorganized

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.simplemobiletools.boomorganized.composables.AppLogo
import com.simplemobiletools.boomorganized.composables.BOButton
import com.simplemobiletools.boomorganized.composables.BoomOrganizedExecuting
import com.simplemobiletools.boomorganized.composables.BoomOrganizedGetCSVAndPreview
import com.simplemobiletools.boomorganized.composables.BoomOrganizedRapAndPhoto
import com.simplemobiletools.boomorganized.composables.BoomOrganizedResumePending
import com.simplemobiletools.boomorganized.composables.BoomOrganizingComplete
import com.simplemobiletools.boomorganized.ui.theme.BoomOrganizedTheme
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.smsmessenger.extensions.subscriptionManagerCompat
import com.simplemobiletools.smsmessenger.helpers.PICK_CSV_INTENT
import com.simplemobiletools.smsmessenger.helpers.PICK_PHOTO_INTENT
import kotlinx.coroutines.flow.StateFlow

@SuppressLint("MissingPermission")
class BoomOrganizedActivity : ComponentActivity() {
    private val viewModel: BoomOrganizedViewModel by viewModels()

    private val onBackPressedCallback by lazy {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                doOnBackPress()
            }
        }
    }

    val subscriptionId: Int by lazy {
        subscriptionManagerCompat().activeSubscriptionInfoList.firstOrNull()?.subscriptionId ?: SmsManager.getDefaultSmsSubscriptionId()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BoomOrganizedTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    BoomOrganizedScreen(
                        viewState = viewModel.state,
                        onScriptEdit = { viewModel.script = it },
                        onAddPhoto = ::addPhoto,
                        onRemovePhoto = viewModel::onClearAttachment,
                        goNext = ::nextScreen,
                        onAddCsv = ::addCSV,
                        resumePending = viewModel::resumeSession,
                        freshStart = viewModel::freshStart,
                        onPauseOrganizing = viewModel::pauseOrganizing
                    )
                }
            }
        }
    }

    override fun onResume() {
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        super.onResume()
    }

    fun doOnBackPress() {
        viewModel.onBackViewState {
            onBackPressedCallback.remove()
            onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        onBackPressedCallback.remove()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        when {
            uri == null || resultCode != Activity.RESULT_OK -> {
                toast("Something went wrong")
            }

            requestCode == PICK_PHOTO_INTENT -> {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                viewModel.updateAttachmentState(uri)
            }

            requestCode == PICK_CSV_INTENT -> {
                viewModel.takeCsv(uri, contentResolver)
            }
        }
    }

    private fun nextScreen() {
        viewModel.loadNextViewState()
    }

    private fun addCSV() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "text/comma-separated-values"
        }
        startActivityForResult(intent, PICK_CSV_INTENT)
    }

    private fun addPhoto() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*"))
        }
        startActivityForResult(intent, PICK_PHOTO_INTENT)
    }
}

@Composable
fun BoomOrganizedScreen(
    viewState: StateFlow<BoomOrganizedViewState>,
    goNext: () -> Unit,
    onAddPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onAddCsv: () -> Unit,
    resumePending: () -> Unit,
    freshStart: () -> Unit,
    onScriptEdit: (String) -> Unit,
    onPauseOrganizing: () -> Unit,
) {
    val state by viewState.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        AppLogo()
        Column(modifier = Modifier.weight(1f)) {
            when (val currentState = state) {
                is BoomOrganizedViewState.RapAndImage -> BoomOrganizedRapAndPhoto(
                    modifier = Modifier.weight(1f),
                    script = currentState.script,
                    imageUri = currentState.photoUri,
                    onAddPhoto = onAddPhoto,
                    onRemovePhoto = onRemovePhoto,
                    onScriptEdit = onScriptEdit
                )

                is BoomOrganizedViewState.CsvAndPreview -> BoomOrganizedGetCSVAndPreview(
                    state = currentState,
                    onAddCsv = onAddCsv,
                )

                is BoomOrganizedViewState.BoomOrganizedExecute -> BoomOrganizedExecuting(
                    contact = currentState.contact,
                    contactCounts = currentState.counts,
                    isPaused = currentState.isPaused,
                    isLoading = currentState.isLoading,
                    onPauseOrganizing = onPauseOrganizing,
                    onResumeOrganizing = resumePending
                )

                is BoomOrganizedViewState.OfferToResume -> BoomOrganizedResumePending(
                    counts = currentState.contactCounts,
                    preview = currentState.preview,
                    imageUri = currentState.photoUri,
                    onReattach = onAddPhoto,
                    onRemovePhoto = onRemovePhoto,
                    onScriptEdit = onScriptEdit
                )

                BoomOrganizedViewState.Uninitiated -> Unit
                BoomOrganizedViewState.OrganizationComplete -> BoomOrganizingComplete()
            }
        }

        // Button control flow logic
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            when (val vState = state) {
                is BoomOrganizedViewState.CsvAndPreview -> {
                    if (vState.csvState is CsvState.Found) {
                        BOButton(text = "Commence to Organizing", onClick = goNext)
                    }
                }

                is BoomOrganizedViewState.OrganizationComplete -> {
                    BOButton(text = "Reset", onClick = goNext)
                }

                is BoomOrganizedViewState.OfferToResume -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BOButton(
                            onClick = freshStart,
                            text = "Start fresh"
                        )

                        BOButton(
                            text = "Resume organizing",
                            onClick = goNext
                        )
                    }
                }

                is BoomOrganizedViewState.BoomOrganizedExecute -> {
                    /* no-op */
                }

                else -> {
                    BOButton(modifier = Modifier.width(120.dp), text = "Next", onClick = goNext)
                }
            }
        }
    }
}

@Preview
@Composable
fun ScreenPreview() {
    Column {
        BoomOrganizedRapAndPhoto(
            script = "one two test one two test three",
            onAddPhoto = {},
            onRemovePhoto = {},
            modifier = Modifier,
            imageUri = null,
            onScriptEdit = {}
        )
    }
}

@Preview
@Composable
fun CsvPreview() {
    Column {
        BoomOrganizedGetCSVAndPreview(
            state = BoomOrganizedViewState.CsvAndPreview(
                photoUri = null,
                csvState = CsvState.Found(
                    listOf(),
                    1,
                    2,
                    3
                ),
                preview = "okay and then i wrote another preview for a stupid composable"
            ),
            modifier = Modifier,
            onAddCsv = {},
        )
    }
}

@Preview
@Composable
fun ExecutePreview() {
    BoomOrganizedViewState.BoomOrganizedExecute("Bob Dobalina", ContactCounts(), false, isLoading = false)
}

@Preview(backgroundColor = 0L)
@Composable
fun AppLogoPreview() {
    AppLogo()
}

@Preview(backgroundColor = 0L)
@Composable
fun PassiveButtonPreview() {
    BOButton(text = "Passive") {

    }
}
