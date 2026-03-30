package com.example.electronicbill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = remember { MainViewModel() }
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) { MainScreen(viewModel) }
            }
        }
    }
}

@Composable
fun MainScreen(vm: MainViewModel) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.addResident() }) {
                Icon(Icons.Default.Add, contentDescription = "新增住戶")
            }
        }
    ) { padding ->
        // 使用 LazyColumn 讓整個 UI 內容可上下滑動
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- 區塊 1: 總帳單輸入 ---
            item {
                Text("電費帳當計算", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = vm.totalAmount, onValueChange = { vm.totalAmount = it }, label = { Text("總金額") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = vm.totalUnits, onValueChange = { vm.totalUnits = it }, label = { Text("總度數") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Button(onClick = { vm.calculate() }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("計算電費") }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            // --- 區塊 2: 公電資訊顯示 ---
            if (vm.publicUnitsResult != 0.0) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                            Text("💡 公電資訊", fontWeight = FontWeight.Bold)
                            Text("公電總度數：${String.format("%.1f", vm.publicUnitsResult)} 度")
                            Text("每人分攤公電費：${String.format("%.1f", vm.publicCostPerPersonResult)} 元")
                        }
                    }
                }
            }

            // --- 區塊 3: 住戶清單 ---
            item {
                Text("住戶電表", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("(以下電費已包含公電費)")
            }

            itemsIndexed(vm.residents) { index, resident ->
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // 住戶名稱自訂與刪除按鈕
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = resident.name,
                                onValueChange = { vm.residents[index] = resident.copy(name = it) },
                                label = { Text("住戶名稱") },
                                modifier = Modifier.weight(1f),
                                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold)
                            )
                            IconButton(onClick = { vm.removeResident(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "刪除", tint = Color.Gray)
                            }
                        }

                        // 度數輸入
                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = resident.prevReading, onValueChange = { vm.residents[index] = resident.copy(prevReading = it) }, label = { Text("前期") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = resident.currReading, onValueChange = { vm.residents[index] = resident.copy(currReading = it) }, label = { Text("當期") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }

                        // 計算結果輸出 (度數 + 金額)
                        if (resident.resultAmount > 0) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Text("📈 個人用電：${String.format("%.1f", resident.usage)} 度", color = MaterialTheme.colorScheme.secondary)
                                Text("💰 應付總額：${resident.resultAmount.toInt()} 元", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // 底部留白，避免 FAB 遮擋
            item { Spacer(modifier = Modifier.height(70.dp)) }
        }
    }
}