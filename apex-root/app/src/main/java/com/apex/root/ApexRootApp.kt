package com.apex.root

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.apex.root.ui.screens.dashboard.DashboardScreen
import com.apex.root.ui.screens.scanresult.ScanResultScreen
import com.apex.root.ui.screens.settings.SettingsScreen
import com.apex.root.ui.theme.ApexRootTheme
import com.apex.root.viewmodel.trusted.ApexViewModel

/**
 * v1.1.1: M3 UI 根 Composable — 导航入口 (原 ApexRootApp, 已重命名避免与 Application 子类冲突)。
 * Dashboard ↔ ScanResult ↔ Settings
 *
 * 修复 P0-K1: 之前 `@Composable fun ApexRootApp()` 与 AndroidManifest 的
 * `android:name=".ApexRootApp"` (期望 Application 子类) 命名冲突, 导致启动崩溃。
 * 现在 Application 子类为 [ApexRootApplication], 本函数仅负责 Compose 导航。
 *
 * 修复 ViewModel 作用域: 此前 DashboardScreen 与 ScanResultScreen 各自通过 `viewModel()`
 * 获取 ApexViewModel 实例, 由于 navigation-compose 将 ViewModelStoreOwner 限定到
 * NavBackStackEntry, 两个屏幕拿到的是不同实例 — ScanResultScreen 永远显示空数据
 * (因为扫描结果只存在于 Dashboard 的 VM 中)。现改为在 NavHost 层 (Activity 作用域)
 * 创建单一 ApexViewModel 并显式传入两个屏幕, 确保状态共享。
 */
@Composable
fun ApexRootNavHost() {
    val navController = rememberNavController()
    // 在 NavHost 作用域 (Activity 级 ViewModelStoreOwner) 创建单一 VM 实例,
    // 让 Dashboard 与 ScanResult 共享同一份扫描状态。
    val sharedApexViewModel: ApexViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "dashboard",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("dashboard") {
            DashboardScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToScanResult = { navController.navigate("scanresult") },
                viewModel = sharedApexViewModel
            )
        }
        composable("scanresult") {
            ScanResultScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = sharedApexViewModel
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
