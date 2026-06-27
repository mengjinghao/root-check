package com.apex.root.ui.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import kotlin.math.cos
import kotlin.math.sin

/**
 * APEX Radar Scanner - 高级扫描动画组件
 * 采用 Liquid Glass 风格，展示实时检测进度
 */
@Composable
fun ApexRadarScanner(
    isScanning: Boolean,
    progress: Float = 0f,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp
) {
    val transition = rememberInfiniteTransition()
    
    // 雷达旋转动画
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    // 脉冲波纹动画
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    // 扫描线渐变位置
    val sweepAngle by animateFloatAsState(
        targetValue = if (isScanning) 360f * progress else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "sweepProgress"
    )
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 外层轨道环
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 2.dp.toPx()
            val radius = (size.toPx() - strokeWidth) / 2
            
            // 轨道背景
            drawCircle(
                color = DeepSurfaceVariant,
                radius = radius,
                style = Stroke(width = strokeWidth)
            )
            
            // 装饰性刻度线
            for (i in 0 until 72) {
                val angle = (i * 5).degreesToRadians()
                val isMajor = i % 18 == 0
                val lineLength = if (isMajor) 12.dp.toPx() else 6.dp.toPx()
                val lineWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                
                val startX = (radius + 4.dp.toPx()) * cos(angle)
                val startY = (radius + 4.dp.toPx()) * sin(angle)
                val endX = (radius + lineLength) * cos(angle)
                val endY = (radius + lineLength) * sin(angle)
                
                drawLine(
                    color = if (isMajor) AccentPurple.copy(alpha = 0.6f) else TextTertiary.copy(alpha = 0.3f),
                    start = Offset(center.x + startX, center.y + startY),
                    end = Offset(center.x + endX, center.y + endY),
                    strokeWidth = lineWidth,
                    cap = StrokeCap.Round
                )
            }
        }
        
        // 脉冲波纹层
        Box(
            modifier = Modifier
                .size(size * 0.85f)
                .clip(RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            // 多层脉冲效果
            repeat(3) { index ->
                val delay = index * 500
                val delayedPulse by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(1500, delay = delay, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
                
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp * (1 - delayedPulse))
                ) {
                    drawCircle(
                        color = AccentBlue.copy(alpha = pulseAlpha * (1 - index * 0.25f)),
                        radius = size.toPx() * 0.4f,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
        
        // 主雷达扫描区域
        Canvas(
            modifier = Modifier
                .size(size * 0.75f)
                .clip(RoundedCornerShape(50))
        ) {
            val radius = size.toPx() * 0.375f
            val centerOffset = Offset(center.x, center.y)
            
            // 背景网格
            drawCircle(
                color = DeepSurfaceBright.copy(alpha = 0.3f),
                radius = radius,
                style = Stroke(width = 1.dp.toPx())
            )
            
            drawCircle(
                color = DeepSurfaceBright.copy(alpha = 0.15f),
                radius = radius * 0.66f,
                style = Stroke(width = 1.dp.toPx())
            )
            
            drawCircle(
                color = DeepSurfaceBright.copy(alpha = 0.1f),
                radius = radius * 0.33f,
                style = Stroke(width = 1.dp.toPx())
            )
            
            // 十字准线
            drawLine(
                color = TextTertiary.copy(alpha = 0.2f),
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1.dp.toPx()
            )
            
            drawLine(
                color = TextTertiary.copy(alpha = 0.2f),
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 1.dp.toPx()
            )
            
            // 扫描扇区
            if (isScanning && progress > 0) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            AccentCyan.copy(alpha = 0.8f),
                            AccentCyan.copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        center = centerOffset
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
            }
            
            // 中心点
            drawCircle(
                color = AccentPurple,
                radius = 6.dp.toPx(),
                center = centerOffset
            )
            
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = 3.dp.toPx(),
                center = centerOffset
            )
        }
        
        // 旋转的雷达边框
        Canvas(modifier = Modifier.size(size * 0.8f)) {
            val radius = size.toPx() * 0.4f
            
            rotate(rotation) {
                drawArc(
                    color = AccentPurple.copy(alpha = 0.6f),
                    startAngle = 0f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                
                drawArc(
                    color = AccentBlue.copy(alpha = 0.4f),
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        
        // 进度百分比文本
        if (isScanning) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "SCANNING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    letterSpacing = 3.sp
                )
            }
        }
    }
}

/**
 * 流体进度指示器 - 用于展示多阶段检测进度
 */
@Composable
fun FluidProgressIndicator(
    stages: List<String>,
    currentStage: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition()
    
    val fluidOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FluidEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .liquidGlass(cornerRadius = 16.dp)
            .padding(20.dp)
    ) {
        // 顶部进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(DeepSurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AccentCyan, AccentBlue, AccentPurple),
                            start = Offset.Zero,
                            end = Offset(Float.POSITIVE_INFINITY, 0f)
                        )
                    )
            )
        }
        
        Spacer(Modifier.height(20.dp))
        
        // 阶段列表
        stages.forEachIndexed { index, stage ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 阶段指示器
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                index < currentStage -> AccentMint
                                index == currentStage -> AccentPurple
                                else -> DeepSurfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < currentStage) {
                        Text(
                            text = "✓",
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (index == currentStage) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.8f))
                        )
                    }
                }
                
                Spacer(Modifier.width(12.dp))
                
                // 阶段文本
                Text(
                    text = stage,
                    fontSize = 13.sp,
                    color = when {
                        index < currentStage -> AccentMint
                        index == currentStage -> TextPrimary
                        else -> TextTertiary
                    },
                    fontWeight = if (index == currentStage) FontWeight.SemiBold else FontWeight.Normal
                )
                
                // 当前阶段显示流动效果
                if (index == currentStage) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(DeepSurfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(2.dp)
                                .align(Alignment.CenterStart)
                                .offset {
                                    androidx.compose.ui.unit.IntOffset(
                                        x = ((fluidOffset * 28).toInt()),
                                        y = 0
                                    )
                                }
                                .background(AccentPurple)
                        )
                    }
                }
            }
            
            if (index < stages.lastIndex) {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private val FluidEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
private fun Float.degreesToRadians(): Float = this * Math.PI.toFloat() / 180f
