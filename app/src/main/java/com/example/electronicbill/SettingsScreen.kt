package com.example.electronicbill

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    // Placeholder screen for future app-level preferences.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.currentLanguage == "zh") "設定" else "Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        // Keep content centered so users can clearly see this module is not finished yet.
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (vm.currentLanguage == "zh") "設定功能開發中..." else "Settings under development...")
        }
    }
}