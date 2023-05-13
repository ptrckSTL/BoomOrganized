@file:OptIn(ExperimentalAnimationApi::class)

package com.simplemobiletools.boomorganized.composables

import android.animation.TimeInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.simplemobiletools.boomorganized.ContactCounts

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ColumnScope.BoomOrganizedExecuting(
    contact: String,
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
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
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
        }
    }
}

@Composable
fun ProgressRows(modifier: Modifier = Modifier, contactCounts: ContactCounts) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.End
    ) {
        ExecutingDataRow(description = "Pending", value = contactCounts.pending, 0)
        Spacer(modifier = Modifier.height(2.dp))
        ExecutingDataRow(description = "Sending", value = contactCounts.sending, 250)
        Spacer(modifier = Modifier.height(2.dp))
        ExecutingDataRow(description = "Sent", value = contactCounts.sent, 500)
    }
}

@Composable
fun ExecutingDataRow(description: String, value: Int, animationOffsetMillis: Int = 0) {
    val easing = AnticipateOvershootInterpolator().toEasing()
    val tweenSpec: FiniteAnimationSpec<IntOffset> = tween(easing = easing, delayMillis = animationOffsetMillis)
    Row(horizontalArrangement = Arrangement.End) {
        Column {
            Text(description)
        }
        Column(modifier = Modifier.width(48.dp), horizontalAlignment = Alignment.Start) {
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    when {
                        targetState > initialState -> ContentTransform(
                            slideIntoContainer(AnimatedContentScope.SlideDirection.Down, tweenSpec),
                            slideOutOfContainer(AnimatedContentScope.SlideDirection.Down, tweenSpec)
                        )

                        else -> ContentTransform(
                            slideIntoContainer(AnimatedContentScope.SlideDirection.Up, tweenSpec),
                            slideOutOfContainer(AnimatedContentScope.SlideDirection.Up, tweenSpec)
                        )
                    }
                }) { number ->
                Row(Modifier.padding(start = 16.dp), horizontalArrangement = Arrangement.End) {
                    Box(
                        Modifier
                            .padding(2.dp)
                            .border(BorderStroke(Dp.Hairline, Color.DarkGray))
                            .padding(2.dp)
                    ) {
                        Text(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            text = number.toString(),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun TimeInterpolator.toEasing() = Easing { x ->
    getInterpolation(x)
}