package com.example.electronicbill

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

private fun formatChartValue(value: Double): String =
    String.format(Locale.getDefault(), "%.2f", value)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(vm: MainViewModel, onBack: () -> Unit) {
    val aggregate = vm.aggregatedData
    val unitData = aggregate.unitData
    val costData = aggregate.costData
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
        if (unitData.isEmpty() || unitData.values.sum() <= 0.0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isZh) "暫無可分析的歷史數據" else "No analyzable data available")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Text(
                        if (isZh) "📊 住戶總用電度數比例" else "📊 Total Units Proportion",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    PieChartWithLegend(data = unitData, unit = if (isZh) "度" else "kWh")
                }

                item { HorizontalDivider() }

                if (costData.isNotEmpty() && costData.values.sum() > 0.0) {
                    item {
                        Text(
                            if (isZh) "💰 住戶總電費支出比例" else "💰 Total Cost Proportion",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        PieChartWithLegend(data = costData, unit = if (isZh) "元" else "NTD")
                    }
                }
            }
        }
    }
}

@Composable
fun PieChartWithLegend(data: Map<String, Double>, unit: String) {
    val isZh = unit == "度" || unit == "元"
    val colors = remember {
        listOf(
            Color(0xFF6200EE),
            Color(0xFF03DAC5),
            Color(0xFFFF0266),
            Color(0xFFF44336),
            Color(0xFF4CAF50),
            Color(0xFFFFC107)
        )
    }
    val filteredData = data.filterValues { it > 0.0 }
    val total = filteredData.values.sum()

    if (filteredData.isEmpty() || total <= 0.0) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(if (isZh) "沒有可繪製的有效資料" else "No valid data")
        }
        return
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(200.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = 0f
                filteredData.values.forEachIndexed { index, value ->
                    val sweepAngle = (value / total * 360.0).toFloat()
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

        filteredData.entries.forEachIndexed { index, entry ->
            val percentage = entry.value / total * 100
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(12.dp).background(colors[index % colors.size]))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${entry.key}: ${formatChartValue(entry.value)} $unit (${formatChartValue(percentage)}%)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
