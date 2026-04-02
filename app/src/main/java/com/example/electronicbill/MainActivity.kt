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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.room.Room
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "electric-db"
        ).build()

        setContent {
            val viewModel = remember { MainViewModel() }

            LaunchedEffect(Unit) {
                viewModel.initData(db)
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(vm = viewModel, db = db)
                }
            }
        }
    }
}

private enum class AppPage {
    MAIN,
    HISTORY,
    ANALYSIS,
    LANGUAGE,
    INSTRUCTIONS,
    SETTINGS,
    DETAIL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: MainViewModel, db: AppDatabase) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isZh = vm.currentLanguage == "zh"
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    var currentPageName by rememberSaveable { mutableStateOf(AppPage.MAIN.name) }
    val currentPage = AppPage.valueOf(currentPageName)

    fun showMainOnly() {
        currentPageName = AppPage.MAIN.name
        scope.launch { drawerState.close() }
    }

    fun showMainWithDrawer() {
        currentPageName = AppPage.MAIN.name
        scope.launch { drawerState.open() }
    }

    fun showDrawerPage(page: AppPage) {
        currentPageName = page.name
        scope.launch { drawerState.close() }
    }

    fun showDetailPage() {
        currentPageName = AppPage.DETAIL.name
        scope.launch { drawerState.close() }
    }

    val isDrawerFeaturePage = currentPage in listOf(
        AppPage.HISTORY,
        AppPage.ANALYSIS,
        AppPage.LANGUAGE,
        AppPage.INSTRUCTIONS,
        AppPage.SETTINGS
    )

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    BackHandler(enabled = drawerState.isClosed && currentPage == AppPage.DETAIL) {
        showMainOnly()
    }

    BackHandler(enabled = drawerState.isClosed && isDrawerFeaturePage) {
        showMainWithDrawer()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentPage != AppPage.DETAIL || drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = if (isZh) "功能選單" else "Menu",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleLarge
                        )
                        HorizontalDivider()
                    }

                    item {
                        DrawerMenuItem(
                            label = if (isZh) "歷史紀錄" else "History",
                            selected = currentPage == AppPage.HISTORY,
                            icon = { Icon(Icons.Default.History, contentDescription = null) },
                            onClick = { showDrawerPage(AppPage.HISTORY) }
                        )
                    }

                    item {
                        DrawerMenuItem(
                            label = if (isZh) "用電分析" else "Analysis",
                            selected = currentPage == AppPage.ANALYSIS,
                            icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                            onClick = { showDrawerPage(AppPage.ANALYSIS) }
                        )
                    }

                    item {
                        DrawerMenuItem(
                            label = if (isZh) "語言設定" else "Language",
                            selected = currentPage == AppPage.LANGUAGE,
                            icon = { Icon(Icons.Default.Language, contentDescription = null) },
                            onClick = { showDrawerPage(AppPage.LANGUAGE) }
                        )
                    }

                    item {
                        DrawerMenuItem(
                            label = if (isZh) "使用說明" else "Instructions",
                            selected = currentPage == AppPage.INSTRUCTIONS,
                            icon = { Icon(Icons.Default.Help, contentDescription = null) },
                            onClick = { showDrawerPage(AppPage.INSTRUCTIONS) }
                        )
                    }

                    item {
                        DrawerMenuItem(
                            label = if (isZh) "設定" else "Settings",
                            selected = currentPage == AppPage.SETTINGS,
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = { showDrawerPage(AppPage.SETTINGS) }
                        )
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = if (isZh) "歷史紀錄快捷套用" else "History Quick Apply",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    if (vm.historyList.isEmpty()) {
                        item {
                            Text(
                                text = if (isZh) "尚無歷史紀錄" else "No history found",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        items(vm.historyList.take(5), key = { it.id }) { record ->
                            NavigationDrawerItem(
                                label = {
                                    Column {
                                        Text(
                                            text = sdf.format(Date(record.date)),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${if (isZh) "總額" else "Total"}: ${record.totalAmount.toInt()}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                selected = false,
                                onClick = {
                                    vm.applyRecord(record)
                                    showMainOnly()
                                },
                                badge = {
                                    IconButton(onClick = { vm.deleteRecord(db, record) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.Red
                                        )
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    ) {
        when (currentPage) {
            AppPage.MAIN -> HomeContent(
                vm = vm,
                db = db,
                onOpenDrawer = {
                    scope.launch { drawerState.open() }
                },
                onOpenDetail = {
                    showDetailPage()
                }
            )

            AppPage.HISTORY -> HistoryScreen(
                vm = vm,
                db = db,
                onBackToDrawer = { showMainWithDrawer() },
                onApplyRecord = { record ->
                    vm.applyRecord(record)
                    showMainOnly()
                }
            )

            AppPage.ANALYSIS -> AnalysisScreen(
                vm = vm,
                onBack = { showMainWithDrawer() }
            )

            AppPage.LANGUAGE -> LanguageScreen(
                vm = vm,
                onBack = { showMainWithDrawer() }
            )

            AppPage.INSTRUCTIONS -> InstructionsScreen(
                vm = vm,
                onBack = { showMainWithDrawer() }
            )

            AppPage.SETTINGS -> SettingsScreen(
                vm = vm,
                onBack = { showMainWithDrawer() }
            )

            AppPage.DETAIL -> DetailScreen(
                vm = vm,
                onBack = { showMainOnly() }
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    label: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selected,
        icon = icon,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    vm: MainViewModel,
    db: AppDatabase,
    onOpenDrawer: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val isZh = vm.currentLanguage == "zh"
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(if (isZh) "⚡ 電費分攤助手 ⚡" else "⚡ Elec. Bill Splitter ⚡")
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.addResident() }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    if (isZh) "帳單資訊" else "Bill Info",
                    style = MaterialTheme.typography.titleMedium
                )

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
                            val success = vm.calculateAndSave(db)
                            if (success) {
                                snackbarHostState.showSnackbar(
                                    if (isZh) "成功存檔" else "Saved successfully"
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
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
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isZh) "📊 本期摘要" else "📊 Summary",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${if (isZh) "每度電費用" else "Price per kWh"}: " +
                                        "$${String.format("%.2f", unitPrice)} ${if (isZh) "元" else ""}"
                            )
                            Text("----------")
                            Text(
                                "${if (isZh) "公電總度數" else "Public Units"}: " +
                                        "${String.format("%.2f", vm.publicUnitsResult)} ${if (isZh) "度" else "kWh"}"
                            )
                            Text(
                                "${if (isZh) "每人公電費" else "Public Cost/Person"}: " +
                                        "$${String.format("%.2f", vm.publicCostPerPersonResult)} ${if (isZh) "元" else ""}"
                            )
                        }
                    }
                }
            }

            itemsIndexed(vm.residents) { index, resident ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = resident.name,
                                onValueChange = {
                                    vm.residents[index] = resident.copy(name = it)
                                },
                                label = { Text(if (isZh) "名稱" else "Name") },
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(onClick = { vm.removeResident(index) }) {
                                Icon(Icons.Default.Clear, contentDescription = "Remove")
                            }
                        }

                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = resident.prevReading,
                                onValueChange = {
                                    vm.residents[index] = resident.copy(prevReading = it)
                                },
                                label = { Text(if (isZh) "前期" else "Prev") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            OutlinedTextField(
                                value = resident.currReading,
                                onValueChange = {
                                    vm.residents[index] = resident.copy(currReading = it)
                                },
                                label = { Text(if (isZh) "當期" else "Curr") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        if (resident.resultAmount > 0) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Text(
                                    "${if (isZh) "📈 個人用電" else "Usage"}: " +
                                            "${String.format("%.1f", resident.usage)} ${if (isZh) "度" else "kWh"}",
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "${if (isZh) "💰 本期總額" else "Total Cost"}: " +
                                            "${resident.resultAmount.toInt()} ${if (isZh) "元" else "NTD"}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }

            if (vm.isCalculated) {
                item {
                    Button(
                        onClick = onOpenDetail,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isZh) "查看詳細計算過程" else "View Detailed Calculation")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}