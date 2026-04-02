package com.example.electronicbill

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

private fun detailNumber(value: Double): String =
    String.format(Locale.getDefault(), "%.2f", value)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(vm: MainViewModel, onBack: () -> Unit) {
    val isZh = vm.currentLanguage == "zh"
    val totalBill = vm.totalAmount.toDoubleOrNull() ?: 0.0
    val totalUnits = vm.totalUnits.toDoubleOrNull() ?: 0.0
    val unitPrice = if (totalUnits > 0) totalBill / totalUnits else 0.0
    val sumIndividualUnits = vm.residents.sumOf { it.usage }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isZh) "電費試算詳細過程" else "Calculation Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = if (isZh) "返回" else "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text(
                            if (isZh) "第一步：計算每度電單價" else "Step 1: Unit Price",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("${if (isZh) "總金額 (A)" else "Total Amount (A)"}: ${detailNumber(totalBill)}")
                        Text("${if (isZh) "總度數 (B)" else "Total Units (B)"}: ${detailNumber(totalUnits)}")
                        Text("${if (isZh) "每度單價" else "Unit Price"}: ${detailNumber(unitPrice)}")
                    }
                }
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text(
                            if (isZh) "第二步：統計個人用電" else "Step 2: Individual Usage",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        vm.residents.forEach { resident ->
                            val price = resident.usage * unitPrice
                            Text(
                                "${resident.name}: ${detailNumber(resident.usage)} ${if (isZh) "度" else "kWh"}, ${detailNumber(price)} ${if (isZh) "元" else "NTD"}"
                            )
                        }

                        Text(
                            "${if (isZh) "住戶用電總和 (C)" else "Residents Total (C)"}: ${detailNumber(sumIndividualUnits)}",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            item {
                val publicTotalCost = vm.publicUnitsResult * unitPrice
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text(
                            if (isZh) "第三步：分配公電費用" else "Step 3: Public Cost Allocation",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("${if (isZh) "公電度數" else "Public Units"}: ${detailNumber(vm.publicUnitsResult)}")
                        Text("${if (isZh) "公電總費用" else "Public Cost"}: ${detailNumber(publicTotalCost)}")
                        Text(
                            "${if (isZh) "每人應付公電費" else "Public Cost per Person"}: ${detailNumber(vm.publicCostPerPersonResult)}",
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
