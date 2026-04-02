package com.example.electronicbill

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(vm: MainViewModel, onBack: () -> Unit) {
    // 取得統計數據
    // Aggregates all saved records into per-resident usage/cost totals.
    val (unitData, costData) = vm.getAggregatedData()
    val isZh = vm.currentLanguage == "zh"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isZh) "用電數據分析" else "Usage Analysis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (unitData.isEmpty()) {
            // Empty-state view when there is no history yet.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (isZh) "暫無歷史數據" else "No Data Available")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 圖表 1: 總用電度數比例
                item {
                    Text(if (isZh) "📊 住戶總用電度數比例" else "📊 Total Units Proportion", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    PieChartWithLegend(data = unitData, unit = if (isZh) "度" else "kWh")
                }

                item { Divider() }

                // 圖表 2: 總電費支出比例
                item {
                    Text(if (isZh) "💰 住戶總電費支出比例" else "💰 Total Cost Proportion", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    PieChartWithLegend(data = costData, unit = if (isZh) "元" else "USD")
                }
            }
        }
    }
}

@Composable
fun PieChartWithLegend(data: Map<String, Double>, unit: String) {
    // Fixed palette with cycling fallback when there are many residents.
    val colors = listOf(Color(0xFF6200EE), Color(0xFF03DAC5), Color(0xFFFF0266), Color(0xFFF44336), Color(0xFF4CAF50), Color(0xFFFFC107))
    val total = data.values.sum()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 繪製圓餅圖
        Box(modifier = Modifier.size(200.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = 0f
                data.values.forEachIndexed { index, value ->
                    // Convert each value into a slice angle based on total sum.
                    val sweepAngle = (value.toFloat() / total.toFloat()) * 360f
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                    startAngle += sweepAngle
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 顯示圖例
        data.keys.forEachIndexed { index, name ->
            val value = data[name] ?: 0.0
            // Show readable percentage next to absolute value.
            val percentage = (value / total * 100).toInt()
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(12.dp).background(colors[index % colors.size]))
                Spacer(Modifier.width(8.dp))
                Text(text = "$name: ${String.format("%.1f", value)} $unit ($percentage%)", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}