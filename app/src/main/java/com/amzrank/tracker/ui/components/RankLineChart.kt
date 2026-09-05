package com.amzrank.tracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amzrank.tracker.data.local.entity.RankRecord
import com.amzrank.tracker.ui.theme.AmazonBlue
import com.amzrank.tracker.ui.theme.AmazonPrimary
import kotlin.math.abs

data class ChartPoint(
    val dateString: String,
    val rank: Int,
    val subRank: Int?,
    val timestamp: Long
)

@Composable
fun RankLineChart(
    records: List<RankRecord>,
    modifier: Modifier = Modifier,
    lineColor: Color = AmazonPrimary,
    showSubRank: Boolean = false
) {
    if (records.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无历史排名数据，请先执行一次更新",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        return
    }

    // 过滤出具有有效排名的点
    val validPoints = remember(records, showSubRank) {
        records.mapNotNull { r ->
            val rankVal = if (showSubRank) (r.subRank ?: r.mainRank) else r.mainRank
            rankVal?.let {
                ChartPoint(
                    dateString = r.dateString,
                    rank = it,
                    subRank = r.subRank,
                    timestamp = r.timestamp
                )
            }
        }.sortedBy { it.timestamp }
    }

    if (validPoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "无有效排名数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        return
    }

    var selectedPoint by remember { mutableStateOf<ChartPoint?>(null) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(validPoints) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(700))
    }

    // 计算 Y 轴上下界（注意：排名越小越好，倒序显示：minRank 在最上方，maxRank 在最下方）
    val minRank = validPoints.minOf { it.rank }
    val maxRank = validPoints.maxOf { it.rank }
    // 增加 10% 留白
    val yPadding = ((maxRank - minRank) * 0.15f).toInt().coerceAtLeast(2)
    val chartTopRank = (minRank - yPadding).coerceAtLeast(1) // 顶部最小数字（最佳名次）
    val chartBottomRank = maxRank + yPadding                 // 底部最大数字（较差名次）

    Column(modifier = modifier.fillMaxWidth()) {
        // 当前选中的点提示条
        selectedPoint?.let { point ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(
                        text = "📅 日期: ${point.dateString}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "🏆 排名: #${point.rank}" + (point.subRank?.let { " (子类目: #$it)" } ?: ""),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmazonPrimary
                    )
                }
            }
        } ?: run {
            Text(
                text = "💡 提示: 曲线越靠上表示排名越靠前（#1在顶部），按住折线滑动可查看具体日期名次",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(validPoints) {
                        detectTapGestures { offset ->
                            val xStep = (size.width - 120.dp.toPx()) / (validPoints.size - 1).coerceAtLeast(1)
                            val index = ((offset.x - 60.dp.toPx()) / xStep).toInt().coerceIn(0, validPoints.lastIndex)
                            selectedPoint = validPoints[index]
                        }
                    }
                    .pointerInput(validPoints) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val xStep = (size.width - 120.dp.toPx()) / (validPoints.size - 1).coerceAtLeast(1)
                                val index = ((offset.x - 60.dp.toPx()) / xStep).toInt().coerceIn(0, validPoints.lastIndex)
                                selectedPoint = validPoints[index]
                            },
                            onDrag = { change, _ ->
                                val xStep = (size.width - 120.dp.toPx()) / (validPoints.size - 1).coerceAtLeast(1)
                                val index = ((change.position.x - 60.dp.toPx()) / xStep).toInt().coerceIn(0, validPoints.lastIndex)
                                selectedPoint = validPoints[index]
                            }
                        )
                    }
            ) {
                val leftPadding = 50.dp.toPx()
                val rightPadding = 20.dp.toPx()
                val topPadding = 20.dp.toPx()
                val bottomPadding = 30.dp.toPx()

                val chartWidth = size.width - leftPadding - rightPadding
                val chartHeight = size.height - topPadding - bottomPadding

                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 10.sp.toPx()
                    isAntiAlias = true
                }

                // 1. 绘制水平网格参考线与 Y 轴刻度（3条水平线）
                val gridSteps = 3
                for (i in 0..gridSteps) {
                    val ratio = i.toFloat() / gridSteps
                    val y = topPadding + chartHeight * ratio
                    // 注意：y=0 对应 chartTopRank（最佳排名，数值小）；y=1 对应 chartBottomRank（数值大）
                    val rankVal = (chartTopRank + (chartBottomRank - chartTopRank) * ratio).toInt()

                    // 参考虚线
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.4f),
                        start = Offset(leftPadding, y),
                        end = Offset(size.width - rightPadding, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )

                    // 绘制 Y 轴文字标签
                    drawContext.canvas.nativeCanvas.drawText(
                        "#$rankVal",
                        10.dp.toPx(),
                        y + 4.dp.toPx(),
                        paint
                    )
                }

                if (validPoints.size == 1) {
                    // 仅有一个点时，居中绘制单点与文字
                    val point = validPoints.first()
                    val cx = leftPadding + chartWidth / 2f
                    val cy = topPadding + chartHeight / 2f

                    drawCircle(color = lineColor, radius = 6.dp.toPx(), center = Offset(cx, cy))
                    drawContext.canvas.nativeCanvas.drawText(
                        "#${point.rank} (${point.dateString})",
                        cx - 40.dp.toPx(),
                        cy - 12.dp.toPx(),
                        paint
                    )
                    return@Canvas
                }

                // 2. 映射所有数据点坐标 (x, y)
                val xStep = chartWidth / (validPoints.size - 1)
                val rankSpan = (chartBottomRank - chartTopRank).toFloat().coerceAtLeast(1f)

                val coords = validPoints.mapIndexed { idx, pt ->
                    val x = leftPadding + idx * xStep
                    // 排名越小（如 #1），归一化越接近 0，y 越接近 topPadding（上方）
                    val normY = (pt.rank - chartTopRank) / rankSpan
                    val y = topPadding + chartHeight * normY.coerceIn(0f, 1f)
                    Offset(x, y)
                }

                // 3. 构建平滑折线 Path
                val strokePath = Path().apply {
                    moveTo(coords[0].x, coords[0].y)
                    for (i in 1 until coords.size) {
                        val prev = coords[i - 1]
                        val curr = coords[i]
                        // 贝塞尔曲线平滑控制点
                        val cX1 = prev.x + (curr.x - prev.x) / 2f
                        val cY1 = prev.y
                        val cX2 = prev.x + (curr.x - prev.x) / 2f
                        val cY2 = curr.y
                        cubicTo(cX1, cY1, cX2, cY2, curr.x, curr.y)
                    }
                }

                // 4. 构建填充阴影 Path
                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(coords.last().x, topPadding + chartHeight)
                    lineTo(coords.first().x, topPadding + chartHeight)
                    close()
                }

                // 绘制渐变填充区域（带进场动画裁剪）
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                        startY = topPadding,
                        endY = topPadding + chartHeight
                    )
                )

                // 绘制主折线
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // 5. 绘制所有数据节点圆圈
                coords.forEachIndexed { i, offset ->
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = offset
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 2.5.dp.toPx(),
                        center = offset
                    )

                    // X 轴日期刻度（每隔若干点显示一个，避免重叠）
                    val stepInterval = (validPoints.size / 5).coerceAtLeast(1)
                    if (i % stepInterval == 0 || i == validPoints.lastIndex) {
                        val dateText = validPoints[i].dateString.takeLast(5) // MM-DD
                        drawContext.canvas.nativeCanvas.drawText(
                            dateText,
                            offset.x - 14.dp.toPx(),
                            size.height - 6.dp.toPx(),
                            paint
                        )
                    }
                }

                // 6. 如果有用户选中的点，绘制十字高亮指示与聚焦圆圈
                selectedPoint?.let { sp ->
                    val sIdx = validPoints.indexOf(sp)
                    if (sIdx in coords.indices) {
                        val sOffset = coords[sIdx]
                        // 垂直高亮线
                        drawLine(
                            color = AmazonBlue,
                            start = Offset(sOffset.x, topPadding),
                            end = Offset(sOffset.x, topPadding + chartHeight),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        )
                        // 外层高亮扩散环
                        drawCircle(
                            color = AmazonBlue.copy(alpha = 0.25f),
                            radius = 10.dp.toPx(),
                            center = sOffset
                        )
                        drawCircle(
                            color = AmazonBlue,
                            radius = 5.dp.toPx(),
                            center = sOffset
                        )
                    }
                }
            }
        }
    }
}
