package com.example.electronicbill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Analytics
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
import androidx.room.Room
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a single Room database instance used by all screens.
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "electric-db"
        ).build()

        setContent {
            val viewModel = remember { MainViewModel() }
            val navController = rememberNavController()

            LaunchedEffect(Unit) {
                // Load history once when app starts, then keep observing updates.
                viewModel.initData(db)
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    // NavHost
                    // Centralized route table for all app screens.
                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") { MainScreen(viewModel, db, navController) }

                        composable("detail") { DetailScreen(viewModel, onBack = { navController.navigateUp() }) }
                        composable("analysis") { AnalysisScreen(viewModel, onBack = { navController.navigateUp() }) }
                        composable("language") { LanguageScreen(viewModel, onBack = { navController.navigateUp() }) }
                        composable("settings") { SettingsScreen(viewModel, onBack = { navController.navigateUp() }) }
                        composable("instructions") { InstructionsScreen(viewModel, onBack = { navController.navigateUp() }) }
                        composable("history") { HistoryScreen(viewModel, db, onBack = { navController.navigateUp() }) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel, db: AppDatabase, navController: androidx.navigation.NavController) {
    // Drawer state controls open/close animation of the side menu.
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    val isZh = vm.currentLanguage == "zh"

    // --- 1. 側邊選單側邊欄 ---
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
                        NavigationDrawerItem(
                            label = { Text(if (isZh) "歷史紀錄" else "History") },
                            selected = false,
                            icon = { Icon(Icons.Default.History, null) }, // 請確保匯入 Icons.Default.History
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    navController.navigate("history") {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )

                        NavigationDrawerItem(
                            label = { Text(if (isZh) "用電分析" else "Analysis") },
                            selected = false,
                            icon = { Icon(Icons.Default.Analytics, null) },
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    navController.navigate("analysis") {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text(if (isZh) "語言設定" else "Language") },
                            selected = false,
                            icon = { Icon(Icons.Default.Language, null) },
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    navController.navigate("language") {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text(if (isZh) "使用說明" else "Instructions") },
                            selected = false,
                            icon = { Icon(Icons.AutoMirrored.Filled.Help, null) },
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    navController.navigate("instructions") {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text(if (isZh) "設定" else "Settings") },
                            selected = false,
                            icon = { Icon(Icons.Default.Settings, null) },
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    navController.navigate("settings") {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = if (isZh) "歷史紀錄" else "History",
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
                                // Reuse a previous bill result by restoring saved input/output values.
                                vm.applyRecord(record)
                                scope.launch { drawerState.close() }
                            },
                            badge = {
                                // Delete only this specific history record.
                                IconButton(onClick = { vm.deleteRecord(db, record) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        content = {
            // --- 2. 主畫面內容 (剛才你漏掉的部分) ---
            val snackbarHostState = remember { SnackbarHostState() }
            
            // Show snackbar when error message changes
            LaunchedEffect(vm.errorMessage) {
                if (vm.errorMessage.isNotEmpty()) {
                    snackbarHostState.showSnackbar(vm.errorMessage)
                }
            }
            
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(if (isZh) "⚡ 電費分攤助手 ⚡" else "⚡ Elec. Bill Splitter ⚡") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { vm.addResident() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 帳單總資訊
                    item {
                        // User enters bill totals from utility statement.
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
                            onClick = {
                                scope.launch {
                                    vm.calculate(db)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        ) {
                            Text(if (isZh) "計算並存檔" else "Calculate & Save")
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    }

                    /* --- 區塊：帳單摘要資訊 --- */
                    if (vm.isCalculated) {
                        item {
                            // Quick summary block after calculation succeeds.
                            val totalBill = vm.totalAmount.toDoubleOrNull() ?: 0.0
                            val totalUnits = vm.totalUnits.toDoubleOrNull() ?: 0.0
                            val unitPrice = if (totalUnits > 0) totalBill / totalUnits else 0.0

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (isZh) "📊 本期摘要" else "📊 Summary",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // 顯示每度電費
                                    Text("${if (isZh) "每度電費用" else "Price per kWh"}: $${String.format(Locale.getDefault(), "%.2f", unitPrice)} ${if (isZh) "元" else ""}")
                                    Text("----------")
                                    Text("${if (isZh) "公電度數" else "Public Units"}: ${String.format(Locale.getDefault(), "%.2f", vm.publicUnitsResult)} ${if (isZh) "度" else "kWh"}")
                                    Text("${if (isZh) "每人公電費用" else "Public Cost/Person"}: $${String.format(Locale.getDefault(), "%.2f", vm.publicCostPerPersonResult)} ${if (isZh) "元" else ""}")
                                }
                            }
                        }
                    }

                    // 住戶清單
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
                                    IconButton(onClick = { vm.removeResident(index) }) {
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
                                    // Show each resident's computed usage and payable amount.
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        Text(
                                            "${if (isZh) "📈 個人用電" else "Usage"}: ${String.format(Locale.getDefault(), "%.1f", resident.usage)} ${if (isZh) "度" else "kWh"}",
                                            color = MaterialTheme.colorScheme.secondary,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "${if (isZh) "💰 本期總額" else "Total Cost"}: ${resident.resultAmount.toInt()} ${if (isZh) "元" else "NTD"}",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 詳細過程按鈕
                    if (vm.isCalculated) {
                        item {
                            // Opens a step-by-step explanation screen for formula transparency.
                            Button(
                                onClick = { navController.navigate("detail") },
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