package com.zsilent.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zsilent.core.design.ZAccent
import com.zsilent.core.design.ZBorder
import com.zsilent.core.design.ZTextPrimary

@Composable
fun ZButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ZAccent,
            contentColor = ZTextPrimary,
            disabledContainerColor = Color(0xFF2A2F38),
            disabledContentColor = Color(0xFF707783)
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text)
    }
}

@Composable
fun ZSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, ZBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ZTextPrimary),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text)
    }
}
