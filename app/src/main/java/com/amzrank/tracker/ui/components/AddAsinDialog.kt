package com.amzrank.tracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amzrank.tracker.data.scraper.AmazonScraper
import com.amzrank.tracker.ui.theme.AmazonPrimary
import com.amzrank.tracker.ui.theme.RankUpGreen

@Composable
fun AddAsinDialog(
    onDismiss: () -> Unit,
    onConfirm: (asin: String, alias: String) -> Unit
) {
    var rawInput by remember { mutableStateOf("") }
    var aliasInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val extractedAsin by remember {
        derivedStateOf {
            AmazonScraper.extractAsinFromUrl(rawInput)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "添加监控 ASIN",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "支持直接输入 10 位 ASIN 码或粘贴亚马逊完整商品分享链接：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = rawInput,
                    onValueChange = {
                        rawInput = it
                        errorMessage = null
                    },
                    label = { Text("ASIN 或 Amazon 链接") },
                    placeholder = { Text("例如 B08N5WRWNW 或 https://amazon.com/dp/...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    isError = errorMessage != null
                )

                // ASIN 识别状态指示
                if (extractedAsin != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "✓ 已成功识别 ASIN: $extractedAsin",
                        color = RankUpGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else if (rawInput.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "未检测到有效的 10 位 ASIN",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = aliasInput,
                    onValueChange = { aliasInput = it },
                    label = { Text("商品备注/别名 (选填)") },
                    placeholder = { Text("例如：主力款耳机、竞品A") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val asin = extractedAsin
                    if (asin == null) {
                        errorMessage = "请输入有效的 10 位 ASIN 码或合规的 Amazon 链接"
                    } else {
                        onConfirm(asin, aliasInput.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmazonPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("添加并立即抓取", color = MaterialTheme.colorScheme.onPrimary)
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
