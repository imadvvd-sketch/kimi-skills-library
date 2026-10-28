package com.imadnote.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.imadnote.app.ui.theme.Spacing

/** Centres content and caps its width on tablets and in landscape; full width on phones. */
fun Modifier.contentWidth(): Modifier = this
    .fillMaxWidth()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = Spacing.maxContentWidth)
    .fillMaxWidth()
