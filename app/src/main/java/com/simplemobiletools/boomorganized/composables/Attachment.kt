package com.simplemobiletools.boomorganized.composables

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun Attachment(modifier: Modifier = Modifier, uri: Uri?, onAddAttachment: () -> Unit, onRemoveAttachment: () -> Unit) {
    var showError by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize()) {
        if (uri != null) {
            Box {
                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth(),
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(uri)
                        .build(),
                    onError = { showError = true },
                    contentDescription = "icon",
                    contentScale = ContentScale.FillWidth,
                )
                Box(
                    Modifier
                        .size(24.dp)
                        .offset((-8).dp, (-8).dp)
                        .graphicsLayer(shadowElevation = 12f, cameraDistance = 4f, shape = CircleShape)
                        .clip(CircleShape)
                        .border(border = BorderStroke(Dp.Hairline, Color.Black), shape = CircleShape)
                        .background(Color.White)
                        .clickable { onRemoveAttachment() }
                        .align(Alignment.TopStart),
                ) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp),
                        imageVector = Icons.Default.Close,
                        tint = Color.Black,
                        contentDescription = "Remove attachment"
                    )
                }
            }
            if (showError) {
                Text("There was an error loading the attachment. Sorry, I don't know why.")
            }
        } else {
            val stroke = Stroke(
                width = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            Row(
                Modifier
                    .height(200.dp)
                    .fillMaxWidth()
                    .clickable { onAddAttachment() }
                    .drawBehind {
                        drawRoundRect(color = Color.White, style = stroke)
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(fontStyle = FontStyle.Italic, text = "attach an image")
            }
        }
    }
}