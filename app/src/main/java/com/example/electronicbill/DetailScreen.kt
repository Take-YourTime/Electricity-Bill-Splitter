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
            // 第一步：單價
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("第一步：計算每度電單價", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("總金額 (A)：$${String.format("%.2f", totalBill)} 元")
                        Text("總度數 (B)：${String.format("%.2f", totalUnits)} 度")
                        Text("每度單價：$${String.format("%.2f", unitPrice)} 元/度")
                    }
                }
            }

            // 第二步：個人用電
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("第二步：統計個人用電", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        vm.residents.forEach { r ->
                            val used = r.usage
                            val price = used * unitPrice
                            // 這裡也統一小數點兩位
                            Text("${r.name}：${String.format("%.2f", used)} 度, ${String.format("%.2f", price)} 元")
                        }

                        Text(
                            "住戶用電總和 (C)：${String.format("%.2f", sumIndividualUnits)} 度",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // 第三步：分配公電
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("第三步：公電費用分配", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Text("公電度數：${String.format("%.2f", vm.publicUnitsResult)} 度")

                        val publicTotalCost = vm.publicUnitsResult * unitPrice
                        Text("公電總費用：$${String.format("%.2f", publicTotalCost)} 元")

                        Text(
                            "每人應付公電費：$${String.format("%.2f", vm.publicCostPerPersonResult)} 元",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(50.dp)) }
        }
    }
}