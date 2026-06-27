# APEX-Root PhantomHide v2.1 - Magisk 隐藏模块

## 概述
APEX-Root PhantomHide 是一个高级的 Magisk/KernelSU 隐藏模块，采用多层防护架构来隐藏 root 痕迹。

## 核心功能

### 1. 文件系统层隐藏 (post-fs-data.sh)
- **tmpfs 覆盖**: 将敏感目录挂载为空的 tmpfs 文件系统
  - `/data/adb/magisk`
  - `/data/adb/ksu`
  - `/dev/.magisk`
  - `/mirror`
  - `/system_root`

- **Bind Mount 隐藏**: 将敏感文件绑定到 `/dev/null`
  - `/system/bin/su`
  - `/system/xbin/su`
  - `/data/adb/magisk.apk`
  - `/data/adb/ksud`

- **假目录创建**: 创建空目录欺骗简单检测

### 2. LD_PRELOAD Hook 层 (libph_spoof.so)
Hook 的系统调用:
- `access()` - 文件访问检查
- `fopen()` - 文件打开
- `open()` / `openat()` - 底层文件打开
- `stat()` / `lstat()` / `fstatat()` - 文件状态查询
- `readlink()` / `readlinkat()` - 符号链接读取
- `faccessat()` - 文件访问检查 (at 版本)
- `opendir()` / `readdir()` - 目录遍历
- `read()` - 文件读取 (过滤/proc 文件)
- `uname()` - 内核版本信息
- `getenv()` - 环境变量访问

隐藏的路径关键词:
- magisk, ksu, apex-root
- magiskhide, resetprop, supolicy
- su 二进制文件路径

### 3. 进程隐藏层 (service.sh)
- 重命名 magisk/ksu 相关进程为 `app_process`
- 修改 `/proc/[pid]/cmdline` 和 `/proc/[pid]/comm`
- 定期清理新出现的 root 痕迹

### 4. 属性伪装层
Spoof 的系统属性:
- `ro.debuggable` = 0
- `ro.secure` = 1
- `ro.build.type` = user
- `ro.build.tags` = release-keys
- `ro.boot.flash.locked` = 1
- `ro.boot.verifiedbootstate` = green
- `ro.boot.vbmeta.device_state` = locked

### 5. eBPF 防火墙集成
- 支持内核 >= 5.10 (Android 12+)
- 系统调用拦截 (openat, statx, getdents64 等)
- BPF 程序自隐藏 (tmpfs 覆盖 `/sys/fs/bpf`)

### 6. 内核符号表保护
- 限制 `/proc/kallsyms` 访问
- 限制 `dmesg` 访问

## 安装方法

### 方式 1: Magisk 刷入
1. 下载 `apex-root-phantomhide-v2.1.zip`
2. 在 Magisk 应用中刷入
3. 重启设备

### 方式 2: 手动安装
```bash
adb push apex-root-phantomhide-v2.1.zip /sdcard/
# 在 Magisk 中选择刷入
```

## 编译 LD_PRELOAD 库 (可选)

如果需要重新编译 `libph_spoof.so`:

```bash
# 设置 NDK 路径
export ANDROID_NDK_ROOT=$HOME/android-ndk-r28

# 运行构建脚本
cd module/
chmod +x build_libph.sh
./build_libph.sh

# 输出：system/lib64/libph_spoof.so
```

## 验证隐藏效果

```bash
# 检查 su 文件是否隐藏
adb shell ls -la /system/bin/su  # 应该显示不存在

# 检查 Magisk 目录
adb shell ls -la /data/adb/magisk  # 应该显示空目录

# 检查系统属性
adb shell getprop ro.debuggable  # 应该返回 0

# 检查进程列表
adb shell ps -A | grep magisk  # 应该无结果
```

## 兼容性

| Android 版本 | 支持 | 备注 |
|------------|------|------|
| Android 10 | ✓ | API 29+ |
| Android 11 | ✓ | |
| Android 12 | ✓ | eBPF 支持 |
| Android 13 | ✓ | |
| Android 14 | ✓ | |
| Android 15 | ✓ | |

| Root 方案 | 支持 |
|----------|------|
| Magisk | ✓ |
| KernelSU | ✓ |
| APatch | ✓ |

## 局限性

### 可以隐藏:
✓ Magisk/KSU 文件和目录
✓ su 二进制文件
✓ Root 管理应用
✓ build.prop 痕迹
✓ /proc/kallsyms 内容
✓ BPF 程序存在
✓ 进程名称

### 部分隐藏:
△ Zygote 注入 (依赖具体实现)
△ 内存扫描 (需要额外配置)
△ 行为检测 (时延分析等)

### 无法隐藏:
✗ 硬件级 attestation (Hardware Key Attestation)
✗ Play Integrity API 的某些检查
✗ 某些游戏的反作弊系统
✗ 银行应用的深度检测

## 故障排除

### 模块不工作
1. 确保 Magisk 版本 >= 28000
2. 检查日志：`adb logcat | grep APEX-Root`
3. 确认 SELinux 状态：`adb shell getenforce`

### 应用仍能检测到 root
1. 尝试清除应用数据
2. 使用 Magisk Hide / DenyList
3. 检查是否有其他 root 模块冲突

### 性能问题
1. LD_PRELOAD 会增加少量开销
2. 如不需要可禁用：删除 `libph_spoof.so`

## 更新日志

### v2.1 (当前版本)
- [新增] faccessat/readlinkat/fstatat hook
- [新增] readdir 目录遍历过滤
- [新增] getenv 环境变量伪装
- [增强] uname 内核版本 spoofing
- [增强] post-fs-data.sh 隐藏更多路径
- [增强] service.sh 双守护进程
- [修复] 线程安全问题 (添加 mutex)

### v2.0
- 初始公开版本
- eBPF + LD_PRELOAD 双层架构
- 基础文件隐藏功能

## 开发者信息

- **作者**: APEX-Root Team (mjh)
- **QQ**: 25544240258
- **微信**: meng4117222
- **项目**: APEX-Root Environment Detection

## 许可证

本模块仅供学习研究使用，请勿用于非法用途。
