package com.simplemobiletools.boomorganized.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.simplemobiletools.smsmessenger.R

@Composable
fun BoomOrganizingComplete() {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(modifier = Modifier.size(100.dp), painter = painterResource(R.drawable.hell_yeah), contentDescription = null)
        Text(text = "That's organizing, baby.")

    }
}