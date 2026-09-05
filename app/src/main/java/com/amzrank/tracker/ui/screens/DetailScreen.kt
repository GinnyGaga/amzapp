package com.amzrank.tracker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.amzrank.tracker.ui.components.RankLineChart
import com.amzrank.tracker.ui.theme.AmazonDark
import com.amzrank.tracker.ui.theme.AmazonPrimary
import com.amzrank.tracker.ui.viewmodel.DetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onNavigateBack: () -> Unit
) {
    val asinItem by viewModel.asinItem.collectAsState()
    val allRecords by viewModel.allRecords.collectAsState()
    val selectedDays by viewModel.selectedDays.collectAsState()
    val showSubRank by viewModel.showSubRank.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val context = LocalContext.current

    // 根据选定的天数过滤记录
    val filteredRecords = if (selectedDays > 0) {
        val cutoff = System.currentTimeMillis() - (selectedDays.toLong() * 24 * 60 * 60 * 1000)
        allRecords.filter { it.timestamp >= cutoff }
    } else {
        allRecords
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = asinItem?.alias ?: "ASIN 排名详情",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // 跳转亚马逊网页
                    asinItem?.asin?.let { asin ->
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.amazon.com/dp/$asin"))
                            context.startActivity(intent)
                        }) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = "在浏览器中查看",
                                tint = Color.White
                            )
                        }
                    }

                    // 立即刷新
                    IconButton(
                        onClick = { viewModel.refreshNow() },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新当前排名",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AmazonDark)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // 顶部商品概述卡片
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = asinItem?.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "ASIN: ${asinItem?.asin ?: ""}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = asinItem?.productTitle ?: "暂无标题信息",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 折线图与筛选选项卡片
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📈 排名走势折线图",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            // 大类/小类切换
                            if (asinItem?.latestSubRank != null) {
                                Row {
                                    Text(
                                        text = if (showSubRank) "小类目" else "大类 BSR",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = AmazonPrimary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { viewModel.setShowSubRank(!showSubRank) }
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }

                        // 日期范围过滤器
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            listOf(7 to "近 7 天", 30 to "近 30 天", 0 to "全部历史").forEach { (days, label) ->
                                FilterChip(
                                    selected = selectedDays == days,
                                    onClick = { viewModel.setSelectedDays(days) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmazonPrimary.copy(alpha = 0.2f),
                                        selectedLabelColor = AmazonDark
                                    )
                                )
                            }
                        }

                        // 绘制折线图
                        RankLineChart(
                            records = filteredRecords,
                            modifier = Modifier.padding(vertical = 8.dp),
                            showSubRank = showSubRank
                        )
                    }
                }
            }

            // 历史明细表标题
            item {
                Text(
                    text = "📋 每日抓取明细记录 (${filteredRecords.size} 条)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 历史明细列表项
            items(items = filteredRecords.reversed(), key = { it.id }) { record ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = record.dateString,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            record.mainCategory?.let {
                                Text(
                                    text = it,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (record.mainRank != null) "#${record.mainRank}" else "无数据",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AmazonPrimary
                            )
                            record.subRank?.let {
                                Text(
                                    text = "小类: #$it",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
