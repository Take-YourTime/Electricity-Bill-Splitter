package com.example.electronicbill

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(vm: MainViewModel, onBack: () -> Unit) {
    val isZh = vm.currentLanguage == "zh"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isZh) "語言設定" else "Language Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        // 使用 Column 並設定 selectableGroup 以利無障礙與邏輯組織
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .selectableGroup()
        ) {
            Text(
                text = if (isZh) "請選擇顯示語言：" else "Select App Language:",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            // 選項 1：繁體中文
            LanguageOptionRow(
                label = "繁體中文 (Traditional Chinese)",
                selected = vm.currentLanguage == "zh",
                onClick = { vm.currentLanguage = "zh" }
            )

            // 選項 2：English
            LanguageOptionRow(
                label = "English (英文)",
                selected = vm.currentLanguage == "en",
                onClick = { vm.currentLanguage = "en" }
            )
        }
    }
}

@Composable
fun LanguageOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    // 使用 Surface 代替直接在 Row 上用 Modifier.clickable
    // Surface 會自動處理 Material 3 的 InteractionSource 與 Indication，避免你遇到的崩潰
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface // 確保背景色與主體一致
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            RadioButton(
                selected = selected,
                onClick = null // 由外部 Surface 統一處理點擊
            )
        }
    }
}