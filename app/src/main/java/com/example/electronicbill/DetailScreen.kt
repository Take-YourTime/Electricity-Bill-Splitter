package com.example.electronicbill

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(vm: MainViewModel, onBack: () -> Unit) {
    // 取得 ViewModel 的計算中間值，如果沒資料（除以零）則設為 0
    val totalBill = vm.totalAmount.toDoubleOrNull() ?: 0.0
    val totalUnits = vm.totalUnits.toDoubleOrNull() ?: 0.0
    val unitPrice = if (totalUnits > 0) totalBill / totalUnits else 0.0

    val sumIndividualUnits = vm.residents.sumOf { r ->
        (r.currReading.toDoubleOrNull() ?: 0.0) - (r.prevReading.toDoubleOrNull() ?: 0.0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("電費試算詳細過程") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 區塊 1: 基礎數據與單價 ---
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("第一步：計算總度單價", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("總金額 (A)：$${totalBill} 元")
                        Text("總度數 (B)：${totalUnits} 度")
                        Text("計算公式：單價 = A / B")
                        Text(
                            "每度單價：$${String.format("%.4f", unitPrice)} 元/度",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // --- 區塊 2: 各戶用電總和 ---
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("第二步：統計住戶用電", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // 列出每一戶
                        vm.residents.forEach { r ->
                            val used = (r.currReading.toDoubleOrNull() ?: 0.0) - (r.prevReading.toDoubleOrNull() ?: 0.0)
                            Text("${r.name}：${used} 度")
                        }

                        Text(
                            "住戶用電總和 (C)：${sumIndividualUnits} 度",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // --- 區塊 3: 公電度數與費用 ---
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("第三步：分配公電", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Text("公電度數 = 總度數(B) - 住戶總和(C)")
                        Text("= ${totalUnits} - ${sumIndividualUnits} = ${String.format("%.1f", vm.publicUnitsResult)} 度")

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("公電總費 = 公電度數 * 每度單價")
                        val publicTotalCost = vm.publicUnitsResult * unitPrice
                        Text("= ${String.format("%.1f", vm.publicUnitsResult)} * ${String.format("%.4f", unitPrice)} = $${String.format("%.1f", publicTotalCost)} 元")

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("每人分攤 = 公電總費 / 住戶人數")
                        Text(
                            "每人應付公電費：$${String.format("%.1f", vm.publicCostPerPersonResult)} 元",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // 底部空白
            item { Spacer(modifier = Modifier.height(50.dp)) }
        }
    }
}