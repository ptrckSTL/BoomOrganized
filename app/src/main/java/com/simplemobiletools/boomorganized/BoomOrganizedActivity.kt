package com.simplemobiletools.boomorganized

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.simplemobiletools.boomorganized.composables.*
import com.simplemobiletools.boomorganized.sheets.ColumnLabel
import com.simplemobiletools.boomorganized.ui.theme.BoomOrganizedTheme
import com.simplemobiletools.smsmessenger.extensions.subscriptionManagerCompat
import com.simplemobiletools.smsmessenger.helpers.PICK_CSV_INTENT
import com.simplemobiletools.smsmessenger.helpers.PICK_PHOTO_INTENT
import kotlinx.coroutines.flow.StateFlow

@SuppressLint("MissingPermission")
class BoomOrganizedActivity : ComponentActivity() {
    private val viewModel: BoomOrganizedViewModel by viewModels()

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
                        goBack = this::doOnBackPress,
                        onAddCsv = ::addCSV,
                        resumePending = viewModel::resumeSession,
                        freshStart = viewModel::freshStart,
                        onPauseOrganizing = viewModel::pauseOrganizing,
                        handleGoogleSheetResult = viewModel::handleGoogleSheetResult,
                        onFilterCreated = {}, // todo
                        onLabelAdded = viewModel::onLabelAdded
                    )
                }
            }
        }
    }

    private fun doOnBackPress() {
        viewModel.onBackViewState {
            onBackPressed()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        when {
            requestCode == PICK_PHOTO_INTENT -> {
                if (uri != null) {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    viewModel.updateAttachmentState(uri)
                }
            }

            requestCode == PICK_CSV_INTENT -> {
                uri?.let {
                    viewModel.takeCsv(it, contentResolver)
                }
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
    goBack: () -> Unit,
    onAddPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onAddCsv: () -> Unit,
    resumePending: () -> Unit,
    freshStart: () -> Unit,
    onScriptEdit: (String) -> Unit,
    onPauseOrganizing: () -> Unit,
    onLabelAdded: (Int, ColumnLabel?) -> Unit,
    onFilterCreated: (Int) -> Unit,
    handleGoogleSheetResult: (FilterableUserSheet?) -> Unit,
) {
    val state by viewState.collectAsState()
    var counts by remember { mutableStateOf(ContactCounts()) }

    when (val vState = state) {
        is BoomOrganizedViewState.BoomOrganizedExecute -> {
            if (!vState.counts.isEmpty()) {
                counts = vState.counts
            }
        }

        is BoomOrganizedViewState.OrganizationComplete -> {
            if (!vState.counts.isEmpty()) {
                counts = vState.counts
            }
        }

        else -> /* no-op */ {}
    }
    BoomScaffold(content = {
        BackHandler { goBack() }
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

                is BoomOrganizedViewState.PreviewOutgoing -> BoomOrganizedGetCSVAndPreview(
                    state = currentState,
                    onAddCsv = onAddCsv,
                    onGoogleSheetSelected = handleGoogleSheetResult
                )

                is BoomOrganizedViewState.BoomOrganizedExecute -> BoomOrganizedExecuting(
                    contact = currentState.contact,
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
                is BoomOrganizedViewState.OrganizationComplete -> BoomOrganizingComplete()

                is BoomOrganizedViewState.RequestLabels ->
                    BoomOrganizedSelectColumns(
                        viewState = currentState,
                        onLabelSelected = onLabelAdded,
                        onCreateFilter = onFilterCreated,
                    )
            }
        }
    }, navigation = {
        // Button control flow logic
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            when (val vState = state) {
                is BoomOrganizedViewState.RapAndImage -> {
                    BOButton(
                        modifier = Modifier.width(120.dp),
                        text = "Next",
                        onClick = goNext
                    )
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

                is BoomOrganizedViewState.PreviewOutgoing -> {
                    if (vState.userSheetState is UserSheetState.Found) {
                        BOButton(text = "Commence to Organizing", onClick = goNext)
                    }
                }

                is BoomOrganizedViewState.BoomOrganizedExecute -> {
                    Box {
                        ProgressRows(
                            modifier = Modifier.align(Alignment.BottomEnd),
                            contactCounts = counts
                        )
                    }
                }

                is BoomOrganizedViewState.OrganizationComplete -> {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                        ProgressRows(contactCounts = counts)
                        BOButton(text = "Reset", onClick = goNext)
                    }
                }

                is BoomOrganizedViewState.Uninitiated -> Unit
                is BoomOrganizedViewState.RequestLabels -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BOButton(
                            onClick = goBack,
                            text = "Previous"
                        )

                        BOButton(
                            text = "Next",
                            onClick = goNext
                        )
                    }
                }
            }
        }
    })
}

@Composable
fun BoomScaffold(content: @Composable ColumnScope.() -> Unit, navigation: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AppLogo()
            content()
        }
        Spacer(Modifier.height(12.dp))
        navigation()
        Spacer(Modifier.height(12.dp))
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
fun ExecutePreview() {
    BoomOrganizedViewState.BoomOrganizedExecute("Bob Dobalina", ContactCounts(), false, isLoading = false)
}

@Preview()
@Composable
fun AppLogoPreview() {
    Column(modifier = Modifier.background(Color.White)) {
        AppLogo()
    }
}

@Preview(backgroundColor = 0L)
@Composable
fun PassiveButtonPreview() {
    BOButton(text = "Passive") {

    }
}
