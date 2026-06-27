package com.apex.root.ui.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * APEX 威胁等级指示器 - 可视化展示安全威胁级别
 * 采用环形渐变 + 动态脉冲效果
 */
@Composable
fun ApexThreatIndicator(
    threatLevel: ThreatLevel,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    showLabel: Boolean = true
) {
    val transition = rememberInfiniteTransition()
    
    // 脉冲动画
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val (color, label, description) = when (threatLevel) {
        ThreatLevel.SAFE -> Triple(AccentMint, "安全", "未检测到威胁")
        ThreatLevel.LOW -> Triple(AccentGold, "低风险", "发现轻微异常")
        ThreatLevel.MEDIUM -> Triple(Color(0xFFFF9800), "中等风险", "存在潜在威胁")
        ThreatLevel.HIGH -> Triple(ErrorRed, "高风险", "检测到严重问题")
        ThreatLevel.CRITICAL -> Triple(Color(0xFF7C4DFF), "严重威胁", "系统已受损")
    }
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 外环轨道
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.toPx() - strokeWidth) / 2
            
            // 背景轨道
            drawCircle(
                color = DeepSurfaceVariant,
                radius = radius,
                style = Stroke(width = strokeWidth)
            )
            
            // 彩色进度弧
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * when (threatLevel) {
                    ThreatLevel.SAFE -> 0.2f
                    ThreatLevel.LOW -> 0.4f
                    ThreatLevel.MEDIUM -> 0.6f
                    ThreatLevel.HIGH -> 0.8f
                    ThreatLevel.CRITICAL -> 1.0f
                },
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = androidx.compose.ui.geometry.Size(
                    size.toPx() - strokeWidth,
                    size.toPx() - strokeWidth
                ),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        // 脉冲光环
        Box(
            modifier = Modifier
                .size(size * 0.85f * pulseScale)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.radialGradient(
                        listOf(
                            color.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // 中心内容
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                letterSpacing = 1.sp
            )
            
            if (showLabel) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * 多层安全盾牌图标 - 动态展示防护状态
 */
@Composable
fun ShieldDefenseIndicator(
    isEnabled: Boolean,
    layers: Int = 3,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    val transition = rememberInfiniteTransition()
    
    val rotation by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shieldRotate"
    )
    
    val glowAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    val shieldColor = if (isEnabled) AccentPurple else TextTertiary
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 发光背景
        Box(
            modifier = Modifier
                .size(size * 0.9f)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.radialGradient(
                        listOf(
                            shieldColor.copy(alpha = glowAlpha * 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // 多层盾牌
        Canvas(modifier = Modifier.size(size * 0.8f)) {
            val centerX = center.x
            val centerY = center.y
            val baseRadius = size.toPx() * 0.35f
            
            for (i in 0 until layers) {
                val layerProgress = (i + 1).toFloat() / layers
                val radius = baseRadius * (0.7f + 0.3f * layerProgress)
                val alpha = 0.3f + 0.5f * layerProgress
                
                // 盾牌形状（简化为圆角矩形）
                val shieldWidth = radius * 1.6f
                val shieldHeight = radius * 2f
                
                drawRoundRect(
                    color = shieldColor.copy(alpha = alpha),
                    topLeft = Offset(centerX - shieldWidth / 2, centerY - shieldHeight / 2),
                    size = androidx.compose.ui.geometry.Size(shieldWidth, shieldHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        x = shieldWidth * 0.3f,
                        y = shieldWidth * 0.3f
                    ),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
            
            // 中心徽章
            drawCircle(
                color = if (isEnabled) AccentMint else ErrorRed,
                radius = 12.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            
            // 状态指示点
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = Offset(centerX, centerY)
            )
        }
        
        // 旋转装饰环
        Canvas(
            modifier = Modifier
                .size(size * 0.95f)
                .padding(8.dp)
        ) {
            rotate(rotation) {
                drawArc(
                    color = shieldColor.copy(alpha = 0.4f),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(size.toPx(), size.toPx()),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                
                drawArc(
                    color = shieldColor.copy(alpha = 0.2f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(size.toPx(), size.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}

/**
 * 数据流量可视化 - 展示实时数据流
 */
@Composable
fun DataFlowVisualizer(
    isActive: Boolean,
    flowRate: Float = 0.5f,
    modifier: Modifier = Modifier,
    height: Dp = 60.dp
) {
    val transition = rememberInfiniteTransition()
    
    val flowOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flowOffset"
    )
    
    val waveHeight by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveHeight"
    )
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(DeepSurfaceVariant.copy(alpha = 0.5f))
    ) {
        val width = size.width
        val maxHeight = size.height
        
        // 绘制多条流动线
        for (i in 0 until 5) {
            val lineAlpha = 0.3f + 0.15f * i
            val yOffset = maxHeight * (0.1f + 0.15f * i)
            val speedMultiplier = 1f + 0.2f * i
            
            val path = androidx.compose.ui.graphics.Path()
            path.moveTo(0f, yOffset)
            
            for (x in 0..width.toInt() step 10) {
                val progress = (x.toFloat() / width + flowOffset * speedMultiplier) % 1f
                val wave = Math.sin(progress * Math.PI * 4).toFloat() * maxHeight * 0.1f * waveHeight
                path.lineTo(x.toFloat(), yOffset + wave)
            }
            
            drawPath(
                path = path,
                color = AccentBlue.copy(alpha = lineAlpha),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        
        // 活动指示器
        if (isActive) {
            drawCircle(
                color = AccentMint,
                radius = 4.dp.toPx(),
                center = Offset(width - 20.dp.toPx(), maxHeight / 2)
            )
        }
    }
}

/**
 * 威胁等级枚举
 */
enum class ThreatLevel {
    SAFE, LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * 获取威胁等级的中文名称
 */
fun ThreatLevel.getDisplayName(): String = when (this) {
    ThreatLevel.SAFE -> "安全"
    ThreatLevel.LOW -> "低风险"
    ThreatLevel.MEDIUM -> "中等风险"
    ThreatLevel.HIGH -> "高风险"
    ThreatLevel.CRITICAL -> "严重威胁"
}
