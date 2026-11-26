---
name: "Hook版"
style: "需要运行时真值（参数/返回值/调用链），或不想改 APK 本体。"
lang: "zh"
---

**触发**：需要运行时真值（参数/返回值/调用链），或不想改 APK 本体。
**退出**：证据足够 → 逆向版/抓包版；模块或脚本完成 → 交付加载方法与撤销方式。

> **本机实测**：真机已 root（Magisk，uid=0）、**已装 LSPosed**（真机侧 `/data/adb/lspd` 有 config/log）；
> **未装 frida-server**（`command -v frida-server` 为空）。所以默认顺序是 LSPosed 优先，Frida 需先补环境。

## 1. 选注入方式

| 方式 | 何时用 | 当前可用性 |
|---|---|---|
| **LSPosed 模块** | 长期驻留、跨重启、Java 层稳定 Hook | ✅ 已验证（小猿 AI 探针 v1~v6） |
| **Frida** | 临时探针、快速迭代、native hook | ❌ 未安装，需先补 frida-server + 客户端 |
| **native inline hook** | 目标在 SO 层 | 可用 Magisk/Zygisk 模块或注入框架实现；本机**当前无现成 native hook 工具**，需先补 |
| **UI 自动化** | 只操作界面 | ✅ `android-a11y-cli`，但不算运行时 Hook |

## 2. LSPosed 模块必知（都踩过）

- `META-INF/xposed/module.prop` 的 `minVersion`/`xposedminversion` **必须 ≥ 93**；
  写 82 会「能装、界面认、但不登记、日志一行没有」，排查极耗时。
- 装完/更新模块后：**重新勾作用域 + 强停目标 App**，否则不生效。
- 日志在**真机侧** `/data/adb/lspd/log/modules_<时间戳>.log`（文件名带时间戳；沙箱里看不到该路径，需 `android-shizuku-cli exec` 读）；
  配置在 `config` 目录的 db 与 **`-wal`**，只读主库会误判「未登记」。
- 日志里出现第三方类助手（如某 Xposed 工具）时，堆栈会被污染，定位要按自己的类名过滤。

## 3. 探针最低要求

每个探针必须能回答「它到底跑没跑」：

- 加载成功日志（包名、进程名、时间、模块版本）
- 目标方法命中计数（0 次也要打）
- 入参与返回值摘要（token/密钥只打长度或 hash，**不打明文**）
- 异常与卸载说明
- 先交**只读版**（不改返回值），确认命中后再做改值版

证据等级：Hook 到的是 **E2**；不能单凭 E2 断言服务端限制，需转抓包版拿 E3/E4。

## 4. 常见边界

- 类未加载：换构造器、`Application`、ClassLoader、接口实现或 native 层；不要循环盲 Hook。
- 方法重载：打完整签名，不只按方法名。
- 多进程：先确认进程名；主进程命中不代表 WebView/播放器进程也命中。
- 方法被内联/被 native 注册：Java Hook 无命中不代表没执行，转 SO 或换观测点。
- 只读探针两轮无命中：换入口或换时机，禁止原样重试第三次。

## 5. 交付

- `<目标>_hook_v<n>_<yyyyMMdd-HHmm>.js|apk` + 加载步骤 + 作用域 + 预期日志样例 + 撤销方式。
- 交付前在真机跑一次并贴真实日志；无日志 = 未验证。
- 只记录真实返回值；把本地改值包装成「已解锁/已授权」属于伪造结论，禁止。

必读：`xposed-module-builder`、`noroot-hook`、`frida-scripts`、`rev-frida`、`android-ui-automation`。
