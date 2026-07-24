package com.apex.root.ui

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.apex.root.core.NativeLibraryLoader
import com.apex.root.ApexRootNavHost
import com.apex.root.ui.theme.ApexRootTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * v1.0.4: M3 UI 入口 — 连接 Qwen 写的 ApexRootApp() Composable。
 * 保留: enableEdgeToEdge + native 预加载 + 全局异常兜底。
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ApexPerms"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge 单独 try — 部分 OEM ROM 自定义 WindowInsets 可能抛异常,
        // 不应因此阻止后续 setContent (会导致全程黑屏)。
        runCatching { enableEdgeToEdge() }
            .onFailure { Log.w(TAG, "enableEdgeToEdge failed (non-fatal)", it) }

        try {
            setContent {
                ApexRootTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        ApexRootNavHost()
                    }
                }
            }
        } catch (e: Throwable) {
            // setContent 失败极少见,但一旦发生原实现只打日志 → 用户看到全程黑屏无提示。
            // 现降级为最简 TextView 错误页,至少让用户知道应用崩溃而非系统卡死。
            Log.e(TAG, "Failed to set Compose content", e)
            showErrorView(e)
        }

        // 预热 native 库：在 IO 线程后台加载 libapex_root.so
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                NativeLibraryLoader.ensureLoaded()
            } catch (e: Throwable) {
                Log.e(TAG, "Native library preload failed (non-fatal)", e)
            }
        }
    }

    /**
     * Compose 加载失败的降级错误页。用原生 TextView 而非 Compose,
     * 确保即使 Compose runtime 完全不可用也能显示。
     */
    private fun showErrorView(e: Throwable) {
        val tv = TextView(this).apply {
            text = "APEX Root 启动失败\n\n${e.javaClass.simpleName}: ${e.message}\n\n" +
                "请尝试清除应用数据或更新到最新版本。"
            setPadding(48, 96, 48, 48)
            textSize = 14f
        }
        setContentView(tv)
    }
}
