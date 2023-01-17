package com.simplemobiletools.boomorganized.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.simplemobiletools.boomorganized.ContactCounts

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ColumnScope.BoomOrganizedExecuting(
    contact: String,
    contactCounts: ContactCounts,
    isLoading: Boolean,
    isPaused: Boolean,
    onResumeOrganizing: () -> Unit,
    onPauseOrganizing: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .weight(1f)
    ) {
        if (isLoading) {
            Row(modifier = Modifier.align(Alignment.Center)) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(
                    targetState = contact,
                    transitionSpec = {
                        if (this.initialState != targetState)
                            slideIntoContainer(
                                towards = AnimatedContentScope.SlideDirection.End,
                                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)
                            ) with slideOutOfContainer(AnimatedContentScope.SlideDirection.End)
                        else ContentTransform(EnterTransition.None, ExitTransition.None)
                    }
                ) {
                    Text(text = contact)
                }
                if (contact.isNotBlank()) Text(text = "Boom. Organized.")

                Spacer(modifier = Modifier.height(32.dp))
                OutlinedButton(onClick = if (isPaused) onResumeOrganizing else onPauseOrganizing) {
                    Text(if (isPaused) "Resume" else "Pause")
                }
            }
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.BottomEnd),
                horizontalAlignment = Alignment.End
            ) {
                ExecutingDataRow(description = "Pending", value = contactCounts.pending)
                Spacer(modifier = Modifier.height(2.dp))
                ExecutingDataRow(description = "Sending", value = contactCounts.sending)
                Spacer(modifier = Modifier.height(2.dp))
                ExecutingDataRow(description = "Sent", value = contactCounts.sent)
            }
        }
    }
}

@Composable
fun ColumnScope.ExecutingDataRow(description: String, value: Int) {
    Row(horizontalArrangement = Arrangement.End) {
        Column() {
            Text(description)
        }
        Column(modifier = Modifier.width(48.dp), horizontalAlignment = Alignment.Start) {
            Text(modifier = Modifier.padding(start = 16.dp), text = value.toString())
        }
    }
}