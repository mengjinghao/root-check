# APEX-Root PhantomHide Module

## 概述
高级 Root 隐藏 Magisk 模块，采用多层防护架构实现最大化的 Root 痕迹隐藏。

## 功能特性

### 核心隐藏能力
- ✅ **文件系统隐藏**: 通过 tmpfs 挂载覆盖隐藏 Magisk/KSU 目录
- ✅ **SU 二进制隐藏**: 绑定挂载/dev/null 覆盖 su 文件
- ✅ **应用包隐藏**: 隐藏 Magisk Manager 应用
- ✅ **LD_PRELOAD Hook**: 拦截文件操作函数调用
- ✅ **进程名隐藏**: 尝试重命名敏感进程
- ✅ **属性伪装**: 修改 ro.debuggable 等检测属性
- ✅ **kallsyms 限制**: 限制内核符号表访问
- ✅ **eBPF 防火墙**: (可选) 系统调用级别过滤

### 支持范围
- **完全隐藏**: Magisk/KSU 文件、su 二进制、build.prop 痕迹
- **部分隐藏**: Zygote 注入、内存扫描、BPF 程序存在
- **无法隐藏**: 行为检测 (时延分析)、硬件级 attestation

## 安装方法

### 前置要求
- Android 10+ (API 29+)
- Magisk 28000+ / KernelSU / APatch
- ARM64 架构设备
- 已解锁 Bootloader 并获取 Root

### 刷入步骤
1. 下载最新版本的 `apex-root-module.zip`
2. 进入 Magisk/KernelSU 管理器
3. 点击"从本地安装"选择 zip 文件
4. 等待安装完成
5. **重启设备** (必须)

### 验证安装
```bash
# 检查模块状态
ls -la /data/adb/modules/apex-root/

# 查看日志
logcat -s APEX-Root

# 测试隐藏效果
adb shell ls /data/adb/magisk  # 应该返回"No such file"
adb shell ls /system/bin/su    # 应该返回"No such file"
```

## 模块结构

```
module/
├── module.prop          # 模块元数据
├── customize.sh         # 安装脚本
├── post-fs-data.sh      # 早期挂载隔离 (fs_data 阶段)
├── service.sh           # 后台服务 (late_start 阶段)
├── uninstall.sh         # 卸载清理
├── update.json          # 自动更新配置
├── sepolicy/
│   └── sepolicy.rule    # SELinux 策略规则
├── bpf/
│   └── .gitkeep         # eBPF 程序目录 (需编译)
├── system/
│   ├── bin/             # 系统二进制
│   └── lib64/
│       └── ph_spoof.cpp # LD_PRELOAD Hook 源码
└── zygisk/              # Zygisk 模块 (可选)
```

## 工作原理

### 1. post-fs-data 阶段 (最早期)
- 卸载 mount namespace 隔离
- tmpfs 覆盖 Magisk/KSU目录
- bind mount 覆盖 su 文件
- 创建假目录欺骗简单检测

### 2. service 阶段 (启动完成后)
- 加载 eBPF 防火墙 (如果内核支持)
- 注入 LD_PRELOAD 库
- 修改系统属性
- 限制 kallsyms
- 启动周期性清理守护进程

### 3. LD_PRELOAD Hook 层
拦截以下函数:
- `access()` - 文件访问检测
- `fopen()` - 文件打开检测
- `open()` / `openat()` - 底层文件操作
- `stat()` / `lstat()` - 文件状态查询
- `readlink()` - 符号链接读取
- `opendir()` - 目录遍历
- `uname()` - 内核版本查询

### 4. eBPF 防火墙 (Android 12+)
- 内核态系统调用过滤
- 比 LD_PRELOAD 更难被检测
- 需要内核版本 >= 5.10

## 兼容性说明

### 已知兼容
- Pixel 6-8 系列 (Android 13-15)
- Samsung S21-S24 (OneUI 5-7)
- Xiaomi 12-14 系列 (MIUI/HyperOS)
- OnePlus 9-12 系列 (OxygenOS)

### 可能不兼容
- Android 9 及以下 (API < 29)
- x86/x86_64 模拟器
- 某些定制 ROM (ColorOS 旧版本等)

## 故障排除

### 模块无法刷入
- 确认使用最新版 Magisk
- 检查 recovery 是否支持刷入
- 尝试在 Magisk 中直接安装

### 隐藏无效
1. 检查日志：`logcat -s APEX-Root`
2. 确认模块已启用并重启
3. 清除检测应用数据后重试
4. 尝试关闭其他冲突模块

### 系统不稳定
1. 进入安全模式禁用模块
2. 通过 TWRP 删除 `/data/adb/modules/apex-root`
3. 报告问题时附上 logcat 日志

## 注意事项

⚠️ **重要警告**:
- 本模块不能保证 100% 绕过所有检测
- 银行类应用可能有额外检测手段
- Google Pay/钱包可能需要额外配置
- 游戏反作弊 (如腾讯 ACE) 可能仍能检测
- 使用风险自负

## 开发信息

### 编译 LD_PRELOAD 库
```bash
cd module/system/lib64
$ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android29-clang++ \
  -shared -fPIC -O2 -o libph_spoof.so ph_spoof.cpp
```

### 编译 eBPF 程序
```bash
cd apex-root/bpf
clang -target bpf -c -O2 apex_firewall.c -o apex_firewall.o
```

### 构建完整模块
```bash
cd apex-root/module
zip -r apex-root-module.zip *
```

## 联系方式

- **开发者**: mjh (APEX-Root Team)
- **QQ**: 25544240258
- **微信**: meng4117222
- **项目仓库**: /workspace/apex-root

## 许可证

本项目仅供学习研究使用，请勿用于非法用途。
