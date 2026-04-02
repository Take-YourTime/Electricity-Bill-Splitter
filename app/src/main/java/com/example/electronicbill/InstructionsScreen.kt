package com.example.electronicbill

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val isZh = vm.currentLanguage == "zh"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isZh) "使用說明" else "User Guide") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 標題
            item {
                Text(
                    if (isZh) "歡迎使用電費分攤助手！" else "Welcome to Bill Splitter!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 第一步：輸入帳單資訊
            item {
                InstructionStep(
                    icon = Icons.Default.ReceiptLong,
                    title = if (isZh) "1. 輸入帳單總資訊" else "1. Enter Bill Info",
                    description = if (isZh) "請在主頁上方輸入台電帳單上的「總金額」與「總用電度數」。" else "Enter the 'Total Amount' and 'Total Units' from your official bill."
                )
            }

            // 第二步：管理住戶
            item {
                InstructionStep(
                    icon = Icons.Default.GroupAdd,
                    title = if (isZh) "2. 管理住戶名單" else "2. Manage Residents",
                    description = if (isZh) "點擊右下角的「+」按鈕新增住戶，您可以點擊名稱欄位來自訂住戶稱呼。" else "Tap the '+' button to add residents. You can customize names by tapping on the name field."
                )
            }

            // 第三步：填寫電表讀數
            item {
                InstructionStep(
                    icon = Icons.Default.EditNote,
                    title = if (isZh) "3. 填寫個別電表讀數" else "3. Enter Meter Readings",
                    description = if (isZh) "請輸入每位住戶電表的「前期」與「當期」讀數，系統會自動算出個人用電度數。" else "Enter 'Previous' and 'Current' readings for each resident to calculate their usage."
                )
            }

            // 第四步：計算與存檔
            item {
                InstructionStep(
                    icon = Icons.Default.Calculate,
                    title = if (isZh) "4. 計算結果與自動存檔" else "4. Calculate & Save",
                    description = if (isZh) "點擊「計算並存檔」後，系統會自動分配公電費並儲存紀錄至歷史清單中。" else "Tap 'Calculate & Save' to allocate public electricity costs and save the record to history."
                )
            }

            // 計算公式說明 (根據您的 C++ 邏輯撰寫)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            if (isZh) "💡 計算原理說明" else "💡 Calculation Logic",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (isZh)
                                "1. 每度電單價 = 總金額 / 總度數\n" +
                                        "2. 公電度數 = 總度數 - 所有個人用電總和\n" +
                                        "3. 每人應付 = (個人用電 * 單價) + (公電總費 / 住戶數)"
                            else
                                "1. Unit Price = Total Price / Total Units\n" +
                                        "2. Public Units = Total Units - Sum of Individual Usages\n" +
                                        "3. Final Cost = (Usage * Price) + (Public Cost / Residents Count)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // 歷史紀錄與分析
            item {
                InstructionStep(
                    icon = Icons.Default.History,
                    title = if (isZh) "5. 歷史紀錄與分析" else "5. History & Analysis",
                    description = if (isZh) "您可以透過左側選單進入「歷史紀錄」代入舊數據，或在「用電分析」查看比例圖表。" else "Access 'History' to reload past data or check charts in 'Analysis' through the side menu."
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun InstructionStep(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}