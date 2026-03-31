package com.example.electronicbill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(applicationContext)

        setContent {
            val viewModel = remember { MainViewModel() }
            val navController = rememberNavController()

            LaunchedEffect(Unit) { viewModel.initData(db) }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(navController = navController, startDestination = "main") {
                        // 主頁面
                        composable("main") {
                            MainScreen(viewModel, db, navController)
                        }

                        // 其餘頁面：將 popBackStack 包裹在 performNavigation 中，確保退出時也會上鎖
                        composable("detail") {
                            DetailScreen(viewModel, onBack = {
                                viewModel.performNavigation { navController.popBackStack() }
                            })
                        }
                        composable("analysis") {
                            AnalysisScreen(viewModel, onBack = {
                                viewModel.performNavigation { navController.popBackStack() }
                            })
                        }
                        composable("language") {
                            LanguageScreen(viewModel, onBack = {
                                viewModel.performNavigation { navController.popBackStack() }
                            })
                        }
                        composable("settings") {
                            SettingsScreen(viewModel, onBack = {
                                viewModel.performNavigation { navController.popBackStack() }
                            })
                        }
                        composable("instructions") {
                            InstructionsScreen(viewModel, onBack = {
                                viewModel.performNavigation { navController.popBackStack() }
                            })
                        }
                        composable("history") {
                            HistoryScreen(viewModel, db, onBack = {
                                viewModel.performNavigation { navController.popBackStack() }
                            })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel, db: AppDatabase, navController: androidx.navigation.NavController) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    val isZh = vm.currentLanguage == "zh"

    // 防護返回鍵：如果選單開著，先關選單
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = if (isZh) "功能選單" else "Menu",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        // 定義側邊欄導覽項目
                        val navItems = listOf(
                            Triple("history", if (isZh) "歷史紀錄" else "History", Icons.Default.History),
                            Triple("analysis", if (isZh) "用電分析" else "Analysis", Icons.Default.Analytics),
                            Triple("language", if (isZh) "語言設定" else "Language", Icons.Default.Language),
                            Triple("instructions", if (isZh) "使用說明" else "Instructions", Icons.Default.Help),
                            Triple("settings", if (isZh) "設定" else "Settings", Icons.Default.Settings)
                        )

                        navItems.forEach { (route, label, icon) ->
                            NavigationDrawerItem(
                                label = { Text(label) },
                                selected = false,
                                icon = { Icon(icon, null) },
                                onClick = {
                                    // 🚀 使用導覽鎖保護側邊欄跳轉
                                    vm.performNavigation {
                                        navController.navigate(route) {
                                            popUpTo("main") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                        scope.launch { drawerState.close() }
                                    }
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = if (isZh) "快速載入" else "Quick Load",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    items(vm.historyList) { record ->
                        NavigationDrawerItem(
                            label = {
                                Column {
                                    Text(sdf.format(Date(record.date)), fontWeight = FontWeight.Bold)
                                    Text("${if (isZh) "總額" else "Total"}: ${record.totalAmount.toInt()}", style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            selected = false,
                            onClick = {
                                // 代入紀錄不涉及頁面跳轉，但為了保險也可以加上導覽鎖檢查
                                if (!vm.isNavigating) {
                                    vm.applyRecord(record)
                                    scope.launch { drawerState.close() }
                                }
                            }
                        )
                    }
                }
            }
        },
        content = {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(if (isZh) "⚡ 電費分攤助手 ⚡" else "⚡ Elec. Bill Splitter ⚡") },
                        navigationIcon = {
                            IconButton(onClick = {
                                // 🎯 修正痛點：只有在「沒有正在導覽」且「選單關閉」時才允許點擊
                                if (!vm.isNavigating && drawerState.isClosed) {
                                    scope.launch { drawerState.open() }
                                }
                            }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { if (!vm.isNavigating) vm.addResident() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(if (isZh) "帳單資訊" else "Bill Info", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = vm.totalAmount,
                                onValueChange = { vm.totalAmount = it },
                                label = { Text(if (isZh) "總金額" else "Total $") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = vm.totalUnits,
                                onValueChange = { vm.totalUnits = it },
                                label = { Text(if (isZh) "總度數" else "Total kWh") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        Button(
                            onClick = { if (!vm.isNavigating) vm.calculate(db) },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        ) {
                            Text(if (isZh) "計算並存檔" else "Calculate & Save")
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    }

                    if (vm.isCalculated) {
                        item {
                            val totalBill = vm.totalAmount.toDoubleOrNull() ?: 0.0
                            val totalUnits = vm.totalUnits.toDoubleOrNull() ?: 0.0
                            val unitPrice = if (totalUnits > 0) totalBill / totalUnits else 0.0

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = if (isZh) "📊 本期摘要" else "📊 Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${if (isZh) "每度電費用" else "Price per kWh"}: $${String.format("%.2f", unitPrice)}")
                                    Text("----------")
                                    Text("${if (isZh) "公電總度數" else "Public Units"}: ${String.format("%.2f", vm.publicUnitsResult)} 度")
                                    Text("${if (isZh) "每人公電費" else "Public Cost/Person"}: $${String.format("%.2f", vm.publicCostPerPersonResult)} 元")
                                }
                            }
                        }
                    }

                    itemsIndexed(vm.residents) { index, resident ->
                        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = resident.name,
                                        onValueChange = { vm.residents[index] = resident.copy(name = it) },
                                        label = { Text(if (isZh) "名稱" else "Name") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { if (!vm.isNavigating) vm.removeResident(index) }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Remove")
                                    }
                                }
                                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = resident.prevReading,
                                        onValueChange = { vm.residents[index] = resident.copy(prevReading = it) },
                                        label = { Text(if (isZh) "前期" else "Prev") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    OutlinedTextField(
                                        value = resident.currReading,
                                        onValueChange = { vm.residents[index] = resident.copy(currReading = it) },
                                        label = { Text(if (isZh) "當期" else "Curr") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }

                                if (resident.resultAmount > 0) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        Text("${if (isZh) "📈 個人用電" else "Usage"}: ${String.format("%.1f", resident.usage)} 度", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium)
                                        Text("${if (isZh) "💰 本期總額" else "Total Cost"}: ${resident.resultAmount.toInt()} 元", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }
                    }

                    if (vm.isCalculated) {
                        item {
                            Button(
                                onClick = {
                                    // 🚀 使用導覽鎖保護主頁按鈕跳轉
                                    vm.performNavigation {
                                        navController.navigate("detail") {
                                            popUpTo("main") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (isZh) "查看詳細計算過程" else "View Detailed Calculation")
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(120.dp)) }
                }
            }
        }
    )
}