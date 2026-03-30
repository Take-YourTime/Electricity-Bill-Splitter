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
import androidx.room.Room
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化 Room 資料庫
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "electric-db"
        ).build()

        setContent {
            val viewModel = remember { MainViewModel() }

            // 新增：導覽控制器 (NavController)
            val navController = rememberNavController()

            // 程式啟動時載入歷史資料
            LaunchedEffect(Unit) {
                viewModel.initData(db)
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // --- 段落：導覽路由配置 (NavHost) ---
                    NavHost(navController = navController, startDestination = "main") {
                        // 主畫面
                        composable("main") {
                            MainScreen(viewModel, db, onNavigateToDetail = {
                                navController.navigate("detail") // 切換到詳細頁
                            })
                        }
                        // 詳細過程頁面
                        composable("detail") {
                            DetailScreen(viewModel, onBack = {
                                navController.popBackStack() // 返回上一頁
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
fun MainScreen(vm: MainViewModel, db: AppDatabase, onNavigateToDetail: () -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    // 1. 側邊選單
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("歷史紀錄", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(vm.historyList) { record ->
                        NavigationDrawerItem(
                            label = {
                                Column {
                                    Text(sdf.format(Date(record.date)), fontWeight = FontWeight.Bold)
                                    Text("總額: ${record.totalAmount.toInt()} 元", style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            selected = false,
                            onClick = {
                                vm.applyRecord(record)
                                scope.launch { drawerState.close() }
                            },
                            badge = {
                                IconButton(onClick = { vm.deleteRecord(db, record) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "刪除", tint = Color.Red)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    ) {
        // 2. 主畫面佈局
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("電費分攤助手") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "選單")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { vm.addResident() }) {
                    Icon(Icons.Default.Add, contentDescription = "新增住戶")
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                /* --- 段落：帳單總資訊 --- */
                item {
                    Text("帳單資訊", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = vm.totalAmount,
                            onValueChange = { vm.totalAmount = it },
                            label = { Text("總金額") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = vm.totalUnits,
                            onValueChange = { vm.totalUnits = it },
                            label = { Text("總度數") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Button(
                        onClick = { vm.calculate(db) },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Text("計算並存檔")
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                }

                /* --- 段落：住戶輸入區 --- */
                item { Text("住戶抄表與結果", style = MaterialTheme.typography.titleMedium) }

                itemsIndexed(vm.residents) { index, resident ->
                    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // 名稱與刪除按鈕
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = resident.name,
                                    onValueChange = { vm.residents[index] = resident.copy(name = it) },
                                    label = { Text("名稱") },
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { vm.removeResident(index) }) {
                                    Icon(Icons.Default.Clear, contentDescription = "移除")
                                }
                            }
                            // 度數輸入框
                            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = resident.prevReading,
                                    onValueChange = { vm.residents[index] = resident.copy(prevReading = it) },
                                    label = { Text("前期") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = resident.currReading,
                                    onValueChange = { vm.residents[index] = resident.copy(currReading = it) },
                                    label = { Text("當期") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }

                            // 修正：回復到上一個版本的樣貌 (清楚的分行顯示)
                            if (resident.resultAmount > 0) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    Text(
                                        "📈 個人用電：${String.format("%.1f", resident.usage)} 度",
                                        color = MaterialTheme.colorScheme.secondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "💰 本期總額：${resident.resultAmount.toInt()} 元 (含公電)",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 新增：詳細過程按鈕 (僅在計算後顯示在最下方)
                if (vm.isCalculated) {
                    item {
                        Button(
                            onClick = onNavigateToDetail,
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("查看詳細計算過程")
                        }
                    }
                }

                // 底部緩衝空間
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}