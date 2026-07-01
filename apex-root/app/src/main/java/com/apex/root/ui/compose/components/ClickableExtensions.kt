package com.apex.root.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * 扩展函数：确保点击区域最小 48x48dp（WCAG 无障碍标准）
 *
 * 用法：
 *   Icon(...).clickableWithMinSize { onClick() }
 *   Box(...).clickableWithMinSize { onClick() }
 */
fun Modifier.clickableWithMinSize(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    this
        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick
        )
}
