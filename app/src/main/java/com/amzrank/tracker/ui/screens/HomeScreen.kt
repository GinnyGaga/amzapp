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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.amzrank.tracker.data.local.entity.AsinItem
import com.amzrank.tracker.ui.components.AddAsinDialog
import com.amzrank.tracker.ui.components.BatchImportDialog
import com.amzrank.tracker.ui.theme.AmazonBlue
import com.amzrank.tracker.ui.theme.AmazonDark
import com.amzrank.tracker.ui.theme.AmazonPrimary
import com.amzrank.tracker.ui.theme.RankDownRed
import com.amzrank.tracker.ui.theme.RankNeutralGray
import com.amzrank.tracker.ui.theme.RankUpGreen
import com.amzrank.tracker.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
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

    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var showBatchImportDialog by remember { mutableStateOf(false) }
    var asinToDelete by remember { mutableStateOf<AsinItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // 根据搜索词精确/模糊过滤当前列表
    val filteredAsins = remember(asins, searchQuery) {
        if (searchQuery.isBlank()) {
            asins
        } else {
            val q = searchQuery.trim()
            asins.filter { item ->
                item.asin.contains(q, ignoreCase = true) ||
                item.alias.contains(q, ignoreCase = true) ||
                (item.productTitle?.contains(q, ignoreCase = true) == true)
            }
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
                            fontSize = 18.sp,
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
                    // 批量导入
                    IconButton(
                        onClick = { showBatchImportDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = "批量导入 ASIN",
                            tint = Color.White
                        )
                    }
                    // 一键复制全部 ASIN
                    IconButton(
                        onClick = {
                            if (asins.isEmpty()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("当前监控列表中没有 ASIN")
                                }
                            } else {
                                val allText = asins.joinToString("\n") { it.asin }
                                clipboardManager.setText(AnnotatedString(allText))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("已复制全部 ${asins.size} 个 ASIN 到剪贴板")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "一键复制全部 ASIN",
                            tint = Color.White
                        )
                    }
                    // 反爬助手
                    IconButton(onClick = onNavigateToWebVerify) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "反爬验证助手",
                            tint = Color.White
                        )
                    }
                    // 全部更新
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

            // 搜索框（当监控列表有数据时常驻显示）
            if (asins.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("精确/模糊搜索 ASIN、别名或商品标题...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "清除搜索",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                if (searchQuery.isNotBlank()) {
                    Text(
                        text = "共找到 ${filteredAsins.size} 个匹配商品",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                    )
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
                            text = "点击右下角【+】添加单个商品，或点击顶部【批量导入】一次性粘贴添加多个 ASIN！",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else if (filteredAsins.isEmpty()) {
                // 搜索无匹配结果
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🔍", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "未找到包含 \"$searchQuery\" 的商品",
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = { searchQuery = "" }) {
                            Text("清空搜索条件")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = filteredAsins, key = { it.asin }) { item ->
                        AsinCard(
                            item = item,
                            onClick = { onNavigateToDetail(item.asin) },
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(item.asin))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("已复制 ASIN: ${item.asin}")
                                }
                            },
                            onRefresh = { viewModel.syncSingle(item.asin) },
                            onDelete = { asinToDelete = item },
                            onVerify = onNavigateToWebVerify
                        )
                    }
                }
            }
        }
    }

    // 删除二次确认弹窗
    asinToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { asinToDelete = null },
            title = {
                Text(
                    text = "删除监控确认",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(text = "确定要停止监控并删除该商品吗？")
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = item.alias,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "ASIN: ${item.asin}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "注：删除后该 ASIN 的所有历史排名走势数据也将一并清除，且不可恢复。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAsin(item.asin)
                        asinToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("确认删除", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { asinToDelete = null }) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
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

    if (showBatchImportDialog) {
        BatchImportDialog(
            onDismiss = { showBatchImportDialog = false },
            onConfirm = { rawInput ->
                viewModel.addAsinsBatch(rawInput)
                showBatchImportDialog = false
            }
        )
    }
}

@Composable
fun AsinCard(
    item: AsinItem,
    onClick: () -> Unit,
    onCopy: () -> Unit,
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
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onCopy() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.asin,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
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

            // 右侧单项操作（复制 / 刷新 / 删除）
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制 ASIN",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
