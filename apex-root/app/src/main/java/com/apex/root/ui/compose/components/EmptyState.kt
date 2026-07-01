package com.apex.root.ui.compose.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apex.root.ui.compose.*

/**
 * 统一空状态组件
 *
 * @param icon 图标（如 Icons.Default.Search）
 * @param title 标题
 * @param description 描述
 * @param actionText 操作按钮文字（可选）
 * @param onAction 操作回调（可选）
 */
@Composable
fun EmptyState(
    icon: ImageVector = Icons.Default.Search,
    title: String,
    description: String = "",
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(AccentPurple.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AccentPurple.copy(alpha = 0.6f),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        if (description.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                description,
                fontSize = 13.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AccentPurple
                )
            ) {
                Text(actionText, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/**
 * 骨架屏 Shimmer 效果
 *
 * @param modifier 修饰符
 * @param width 宽度
 * @param height 高度
 * @param cornerRadius 圆角
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 16.dp,
    cornerRadius: Dp = 8.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.3f),
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.3f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset(
            translateAnim * 300f - 300f, 0f
        ),
        end = androidx.compose.ui.geometry.Offset(
            translateAnim * 300f, 0f
        )
    )

    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

/**
 * 骨架屏卡片 — 用于列表加载占位
 */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        ShimmerBox(height = 20.dp, cornerRadius = 6.dp, modifier = Modifier.fillMaxWidth(0.6f))
        Spacer(Modifier.height(8.dp))
        ShimmerBox(height = 14.dp, cornerRadius = 6.dp, modifier = Modifier.fillMaxWidth(0.9f))
        Spacer(Modifier.height(4.dp))
        ShimmerBox(height = 14.dp, cornerRadius = 6.dp, modifier = Modifier.fillMaxWidth(0.7f))
    }
}
