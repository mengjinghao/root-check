package com.apex.root

import android.app.Application
import android.content.Context
import android.util.Log
import com.apex.root.core.NativeLibraryLoader
import com.apex.root.core.notification.Notifier
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * v1.1.1: 全局 Application 子类。
 *
 * 修复 P0-K1: AndroidManifest.xml 之前用 `android:name=".ApexRootApp"` 把
 * `@Composable fun ApexRootApp()` 当作 Application 类，运行时 ClassLoader 找不到
 * Application 子类会抛 ClassNotFoundException 导致 App 启动即崩溃。
 *
 * 现在 Manifest 指向 `.ApexRootApplication`，本类负责：
 *  - 创建通知通道 (Notifier.createChannels)
 *  - 后台预热 native 库 (NativeLibraryLoader.ensureLoaded)
 *  - 提供 applicationScope 给非 ViewModel 生命周期使用 (替代 GlobalScope 反模式)
 *  - 提供 [appContext] 全局 application context (FIX-D1: 供 PackageDetector 等
 *    无 Context 入口的检测器使用)
 */
class ApexRootApplication : Application() {

    companion object {
        private const val TAG = "ApexRootApp"

        @Volatile
        private var instance: ApexRootApplication? = null

        @JvmStatic
        fun get(): ApexRootApplication = instance
            ?: error("ApexRootApplication not yet created. Access after Application.onCreate().")

        /**
         * 全局 application context。在 [onCreate] 中赋值。
         *
         * FIX-D1: 供 PackageDetector / Shizuku 检测等无 Context 入口的检测器使用,
         * 避免在 Repository / NativeBridge 中到处传递 Context。
         *
         * 安全修复: 原实现用 `lateinit var instance`,getter 直接 `instance.applicationContext`,
         * 若在 Application.onCreate 之前 (如 ContentProvider 初始化阶段) 被访问会抛
         * UninitializedPropertyAccessException 导致启动崩溃。现改为 nullable + 安全 getter,
         * 未就绪时返回 null,调用方需处理 null (回退到其他 Context 来源或跳过检测)。
         */
        @JvmStatic
        val appContext: Context?
            get() = instance?.applicationContext

        /**
         * 获取 appContext,若未就绪则抛出带清晰提示的 IllegalStateException
         * (而非 UninitializedPropertyAccessException),便于定位调用时机问题。
         */
        @JvmStatic
        fun requireAppContext(): Context =
            instance?.applicationContext
                ?: error("ApexRootApplication.appContext accessed before onCreate() — " +
                    "move the call after Application creation.")
    }

    /**
     * 全局 Application 作用域。生命周期与进程一致，用于替代 GlobalScope 反模式。
     * 自带 CoroutineExceptionHandler，避免未捕获异常导致崩溃。
     */
    val applicationScope: CoroutineScope by lazy {
        val handler = CoroutineExceptionHandler { _, t ->
            Log.e(TAG, "Uncaught coroutine exception in applicationScope", t)
        }
        CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. 创建通知通道 (Android 8+ 必需)
        try {
            Notifier.createChannels(this)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to create notification channels", t)
        }

        // 2. 后台预热 native 库，避免首次扫描卡 UI
        applicationScope.launch(Dispatchers.IO) {
            try {
                NativeLibraryLoader.ensureLoaded()
            } catch (t: Throwable) {
                Log.e(TAG, "Native library preload failed (non-fatal)", t)
            }
        }

        Log.i(TAG, "ApexRootApplication initialized")
    }
}
