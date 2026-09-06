package com.amzrank.tracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amzrank.tracker.data.scraper.AmazonScraper
import com.amzrank.tracker.ui.theme.AmazonPrimary
import com.amzrank.tracker.ui.theme.RankUpGreen

@Composable
fun BatchImportDialog(
    onDismiss: () -> Unit,
    onConfirm: (rawInput: String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var inputText by remember { mutableStateOf("") }

    val detectedAsins = remember(inputText) {
        if (inputText.isBlank()) emptyList()
        else AmazonScraper.extractAllAsinsFromText(inputText)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "批量导入 ASIN",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "支持直接粘贴多行 ASIN、包含多个商品链接的大段文本或表格数据：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        val clip = clipboardManager.getText()?.text
                        if (!clip.isNullOrBlank()) {
                            inputText = clip
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "从剪贴板粘贴",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("从剪贴板一键粘贴")
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("ASIN 列表或文本") },
                    placeholder = { Text("例如：\nB08N5WRWNW\nB07XJ8C8F5\nhttps://amazon.com/dp/B09XYZ1234") },
                    minLines = 4,
                    maxLines = 7,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 实时识别状态展示
                if (detectedAsins.isNotEmpty()) {
                    Surface(
                        color = RankUpGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "✓ 已成功识别出 ${detectedAsins.size} 个有效 ASIN",
                                color = RankUpGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = detectedAsins.take(6).joinToString(", ") + (if (detectedAsins.size > 6) " 等..." else ""),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
                } else if (inputText.isNotBlank()) {
                    Text(
                        text = "未在输入内容中识别出有效的 10 位 ASIN 码",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (detectedAsins.isNotEmpty()) {
                        onConfirm(inputText)
                    }
                },
                enabled = detectedAsins.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = AmazonPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (detectedAsins.isEmpty()) "导入监控" else "导入监控 (${detectedAsins.size}个)",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

