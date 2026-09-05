package com.amzrank.tracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.amzrank.tracker.data.local.entity.AsinItem
import com.amzrank.tracker.ui.components.AddAsinDialog
import com.amzrank.tracker.ui.theme.AmazonBlue
import com.amzrank.tracker.ui.theme.AmazonDark
import com.amzrank.tracker.ui.theme.AmazonPrimary
import com.amzrank.tracker.ui.theme.RankDownRed
import com.amzrank.tracker.ui.theme.RankNeutralGray
import com.amzrank.tracker.ui.theme.RankUpGreen
import com.amzrank.tracker.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToDetail: (asin: String) -> Unit,
    onNavigateToWebVerify: () -> Unit
) {
    val asins by viewModel.asins.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncProgressText by viewModel.syncProgressText.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Amazon 排名监控",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = Color.White
                        )
                        Text(
                            text = "已监控 ${asins.size} 个商品 · 每日自动更新",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToWebVerify) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "反爬验证助手",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { viewModel.syncAll() },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "全部更新",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmazonDark
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AmazonPrimary,
                contentColor = AmazonDark
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "添加 ASIN")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 同步进度提示条
            AnimatedVisibility(visible = isSyncing && syncProgressText != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = AmazonPrimary
                    )
                    Text(
                        text = syncProgressText ?: "",
                        fontSize = 12.sp,
                        color = AmazonDark,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // 防爬人机验证提示横幅
            val hasCaptcha = asins.any { it.lastStatus == "CAPTCHA" }
            if (hasCaptcha) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onNavigateToWebVerify() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🛡️", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "检测到亚马逊防爬拦截",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "点击进入内置浏览器浏览一次，即可自动同步 Cookies 恢复正常抓取",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            if (asins.isEmpty()) {
                // 空状态提示
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📦",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "尚未添加任何 ASIN 监控",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击右下角的【+】按钮，添加您想监控的亚马逊商品 ASIN 或链接即可开始！",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = asins, key = { it.asin }) { item ->
                        AsinCard(
                            item = item,
                            onClick = { onNavigateToDetail(item.asin) },
                            onRefresh = { viewModel.syncSingle(item.asin) },
                            onDelete = { viewModel.deleteAsin(item.asin) },
                            onVerify = onNavigateToWebVerify
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAsinDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { asin, alias ->
                viewModel.addAsin(asin, alias)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AsinCard(
    item: AsinItem,
    onClick: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
    onVerify: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 商品主图
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.alias,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 商品信息与最新排名
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.alias,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = item.asin,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // BSR 大类排名与涨跌状态
                if (item.latestMainRank != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${item.latestMainRank}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AmazonPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "in ${item.latestMainCategory ?: "Category"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 涨跌指示
                    item.rankDelta?.let { delta ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            when {
                                delta > 0 -> {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = "上升",
                                        tint = RankUpGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "较昨日上升 $delta 名",
                                        fontSize = 11.sp,
                                        color = RankUpGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                delta < 0 -> {
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = "下降",
                                        tint = RankDownRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "较昨日下降 ${kotlin.math.abs(delta)} 名",
                                        fontSize = 11.sp,
                                        color = RankDownRed,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.TrendingFlat,
                                        contentDescription = "持平",
                                        tint = RankNeutralGray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "排名持平",
                                        fontSize = 11.sp,
                                        color = RankNeutralGray
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // 状态标记 (待更新 / 无排名 / 遇到验证码 / 错误)
                    when (item.lastStatus) {
                        "NO_RANK" -> {
                            Text(
                                text = "📦 暂无销售榜排名 (新上架/冷门款)",
                                fontSize = 12.sp,
                                color = AmazonBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        "CAPTCHA" -> {
                            Text(
                                text = "⚠️ 触发防爬验证，点击前往",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onVerify() }
                            )
                        }
                        "ERROR" -> {
                            Text(
                                text = "❌ ${item.errorMessage ?: "更新失败，点击右侧重试"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        "NOT_FOUND" -> {
                            Text(
                                text = "🔍 未找到商品页面，请检查 ASIN",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {
                            Text(
                                text = "⏳ 正在排队抓取排名...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // 更新时间戳
                if (item.lastUpdated > 0) {
                    val timeStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(item.lastUpdated))
                    Text(
                        text = "更新于 $timeStr",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // 右侧单项操作
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
