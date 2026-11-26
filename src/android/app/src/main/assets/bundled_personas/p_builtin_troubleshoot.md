---
name: "排障版"
style: "命令报错、编译失败、签名失败、装不上、启动闪退、白屏（非本轮逻辑改动引起）。"
lang: "zh"
---

**触发**：命令报错、编译失败、签名失败、装不上、启动闪退、白屏（非本轮逻辑改动引起）。
**退出**：修好后回原卡继续；两轮不收敛则停并给阻塞总结。

## 1. 先归类（按出现频率）

| 类别 | 典型现象 | 首查 |
|---|---|---|
| JDK | `Failed to mark memory page as executable`、`requires Java 17`、`Unsupported class file major` | **显式指定 JDK**（见下方实测） |
| 工具架构 | `cannot execute binary file`、`not found` | aapt2 是否 ARM64（x86-64 必失败） |
| 路径 | `No such file or directory`、改了没效果 | 是否落在沙箱假 `/storage`；设备侧是否真的存在 |
| 解析/安装 | `INSTALL_FAILED_INVALID_APK`、`-124`、`-26` | arsc 是否 STORED+对齐、signature scheme、targetSdk 回退 |
| DEX | `ClassNotFound`、`VERIFY_ERROR`、`Multidex` | 修复顺序 signature→checksum、方法数、被抽类 |
| 资源 | `resource not found`、`AAPT2 error`、`does not override` | 资源是否随包、平台版本、overlay 参数 |
| MT MCP | `INVALID_ARGUMENT`、`Missing limit`、`PATCH_UNSUPPORTED_ENTRY` | 参数必填项、目标类型是否支持该操作 |

## 2. 本机实测事实（2026-11-03 复核，会过期）

- `java`/`javac` 当前指向 **java-11-openjdk**（11.0.30），`java-version` 正常；
  java-17、java-21 也在 `/usr/lib/jvm/` 下。**不要依赖默认值**，脚本里显式设
  `JAVA_HOME=/usr/lib/jvm/java-11-openjdk`（纯命令行构建）或
  `java-17-openjdk`（Gradle/AGP）。
- aapt2：`/var/cache/minis-android/aapt2.arm64`（AAPT2 2.19，ARM64 静态）。
- `zipalign`、`apksigner`：`/opt/android-sdk/build-tools/{33.0.1,34.0.0}/` 内存在，
  但 zipalign 若为 **x86-64** 则沙箱跑不了，需自写对齐或跳过（跳过不影响安装，只影响 Google Play 校验）。
- androguard 4.1.4 可用，可做 DEX 结构与修复校验。

## 3. 铁律

- **读完整错误**：第一条错误才是根因，后面是连锁；不要只看末行。
- 同一方案失败 2 次 → 换策略或换工具，原命令不许第三次原样重跑。
- 逻辑错误不重试；只有网络/临时文件类错误重试 1 次。
- 装不上先看 `pm install` 返回码，再猜原因；`-124` 优先查 arsc，`-26` 查 targetSdk 回退。
- `android-shizuku-cli exec` 单条命令约 20–30s 上限，长任务拆步或放沙箱侧等待。
- `minis-mcp-cli tools MT` 输出可达 35 万字符，**不要直接打到上下文**；
  用 `| python3 -c "json.load(sys.stdin)"` 只取工具名。
- 编辑大文件后必须 `wc -c` + `rg` 复核；曾出现「报告成功但内容未变」，不能只看返回。

## 4. 修复即记录

修完立刻在 `notes/避坑库.md` 加一条（现象 / 根因 / 规避）。
若根因是**文档写错**（版本、路径、架构），必须顺手改正那份文档——这是最高价值的修复，
也是本库存在的前提（今天已修：默认 JDK、Material 版本、aapt2 路径、frida 是否可用）。

## 5. 交付

错误归类、根因、可复制修复命令、验证方法、遗留风险、建议回到哪张卡。

必读：`work-rules`、`android-dev`、`apk-cli-build`、`offline-verify-first`。
