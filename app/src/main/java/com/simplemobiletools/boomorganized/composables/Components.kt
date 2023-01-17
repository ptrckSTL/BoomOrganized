package com.simplemobiletools.boomorganized.composables

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.simplemobiletools.smsmessenger.R

@Composable
fun EditScriptBox(
    editable: Boolean,
    savedScript: String,
    onScriptEdit: (String) -> Unit,
) {
    var userInput by remember { mutableStateOf(savedScript) }
    OutlinedTextField(
        label = { Text("Rap goes here") },
        enabled = editable,
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .verticalScroll(rememberScrollState()),
        value = userInput,
        onValueChange = {
            userInput = it
            onScriptEdit(userInput)
        }
    )
}

@Composable
fun BOButton(modifier: Modifier = Modifier, text: String, onClick: () -> Unit) {
    Button(
        border = BorderStroke(1.dp, color = colorResource(id = R.color.md_amber_800_dark)),
        colors = ButtonDefaults.buttonColors(Color.Black),
        modifier = modifier,
        onClick = { onClick() },
        content = { Text(color = colorResource(id = R.color.md_orange_500_dark), text = text) }
    )
}

@Composable
fun RapWithImagePreview(preview: String, photoUri: Uri?) {
    Column {
        Text(
            modifier = Modifier.padding(start = 8.dp),
            fontSize = 10.sp,
            text = "Preview:"
        )
        Column(
            modifier = Modifier
                .border(BorderStroke(1.dp, Color.DarkGray), RoundedCornerShape(size = 8.dp))
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = preview
            )
            if (photoUri != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AttachmentPreview(photoUri = photoUri)
                }
            } else {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    text = "[No image]"
                )
            }
        }
    }
}

@Composable
fun BoxScope.AttachmentPreview(photoUri: Uri?) {
    AsyncImage(
        onError = {},
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .width(80.dp),
        model = ImageRequest.Builder(LocalContext.current)
            .data(photoUri)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Inside,
    )
}

@Composable
fun AppLogo() {
    val headerColor = if (isSystemInDarkTheme()) Color.White else Color.Black
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "boom", fontSize = 32.sp, color = headerColor, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Thin)
        Image(
            modifier = Modifier
                .rotate(295f)
                .size(44.dp)
                .padding(start = 4.dp, end = 12.dp, bottom = 2.dp),
            colorFilter = ColorFilter.tint(color = headerColor),
            painter = rememberVectorPainter(image = ImageVector.vectorResource(id = R.drawable.rolling_bomb_svgrepo_com)),
            contentDescription = null
        )
        Text(text = "rganized", fontSize = 32.sp, color = headerColor, modifier = Modifier.offset(x = (-12).dp), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ScriptInformation() {
    Row {
        Icon(
            Icons.Default.Info, contentDescription = null, modifier = Modifier
            .padding(2.dp)
            .size(12.dp)
        )
        Text(text = "You can use firstName or lastName as placeholders in your rap.", fontSize = 10.sp)
    }
}
