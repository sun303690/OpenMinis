---
name: reverse-engineer-anything
description: "逆向分析总纲：面对已编译的产物（APK/JAR/ELF/.so/Electron 包/PyInstaller/固件）时，先判定目标类型、再选正确的第一条工具，用产物证据而非猜测来回答「它怎么实现的」。含目标路由决策表、星辰工具映射、证据工作流与还原重建路径。"
category: 逆向工程
author: morluto (REA) / 星辰适配
source: https://github.com/morluto/rea
version: "1.1.0"
triggers: 逆向,反编译,还原实现,怎么实现的,分析这个so,分析bin,脱壳,dump,还原算法,对比版本
---

# 逆向分析总纲（REA 方法论 · 星辰工具版）

核心原则只有一条：**结论必须由产物证据支撑**。看到一段可疑字符串就断言"这是加密函数"，
和打开函数体确认它在做 AES-CBC 是两件事。本技能的作用是让每一步都落在证据上。

## 什么时候用

用：

- 问题依赖**已发布的二进制/包**才能回答（"这个 APK 的登录是怎么签名的"）
- 需要**反编译**或**运行时观测**才能确认（静态看不到的注册、动态加载）
- 要做**版本对比**（新版加了什么检测）
- 要做**重建**（照着别人的功能写出等价实现）

不用：

- 源码就在仓库里、能直接读 → 用普通代码阅读，别套逆向流程
- 只是想跑起来看看 → 先跑，别急着拆

## 第一步：路由目标（不要跳步）

拿着文件先判断类型，再选工具。选错第一步会浪费十几轮。

| 目标 | 第一条工具 | 说明 |
|---|---|---|
| APK / AAB | `apk_info` → `mt_apk_session_create` | 先看包名、版本、权限、dex 数量；再开 session 逐项深入 |
| JAR / AAR / DEX 单文件 | `dex_list` → `dex_smali` | 直接列类与反编译 smali |
| 普通 ZIP / 各种安装包 | `mt_apk_open` → `mt_apk_list` | 先列成员清单与大小，别急着全解 |
| ELF / .so / 可执行文件 | `mt_apk_native_inspect` → `mt_apk_native_list_items` | 先看架构、段表、导入导出符号 |
| Electron / asar | 解出 asar 后用 `mt_apk_search` 搜关键字 | `app.asar` 本质是归档，`mt_apk_list` 能列 |
| 前端打包产物（webpack/vite） | 搜 `sourceMappingURL`、`__webpack_require__` | 有 sourcemap 就直接还原，别硬啃 |
| PyInstaller / Bun / Deno 单文件 | `mt_apk_list` 看尾部结构 | 应用层是归档里的脚本，bootloader 只是前缀 |
| 固件 / 镜像 | `mt_apk_list` 找 squashfs/cramfs 偏移 | 用 `execute_command` + binwalk 更省事 |
| 已跑起来的进程 | `device_toolbox` + Frida 技能 | 静态看不到的注册/解密走运行时 |

**判定不要靠猜**：`file`、`execute_command` 里的 `xxd | head`、读魔数（`PK\x03\x04`、`\x7fELF`、
`0xE9` ESP 镜像）都是几秒钟的事，比试错快。

## 星辰工具映射

本技能源自 REA（一套 MCP 服务），原文里的工具名在这里替换为星辰的实际工具：

| REA 原文 | 星辰等价 | 用途 |
|---|---|---|
| `open_binary` | `mt_apk_session_create` / `mt_apk_open` | 打开产物，建立会话 |
| `inspect_artifact` | `mt_apk_list` + `mt_apk_read_text` / `mt_apk_read_bytes` | 成员清单、单文件读取 |
| `inspect_android_package` | `apk_info` + `apk_scan` | 包身份、清单、风险扫描 |
| `search_android_classes` | `mt_apk_dex_read_class_outline` / `mt_apk_search` | 找类 |
| `inspect_android_method` | `dex_smali` / `mt_apk_dex_read_class_outline` | 看方法实现 |
| `trace_android_references` | `dex_xref` / `mt_apk_dex_xref` | 谁调用了它 |
| Android 资源 / 清单细节 | `arsc_read` / `mt_apk_resource_read` / `mt_apk_resource_xref` | 字符串、布局、资源引用 |
| 原生代码反汇编 | `mt_apk_native_disassemble` / `mt_apk_native_inspect` | ARM/ARM64 指令 |
| 原生符号与交叉引用 | `mt_apk_native_xref` / `mt_apk_native_map_address` | 地址映射、调用关系 |
| 原生打补丁 | `mt_apk_native_patch_instructions` / `mt_apk_native_patch_string` | 改指令/字符串（先 dryRun） |
| 重打包与签名 | `mt_apk_build` / `apk_replace` / `apk_sign` / `smali_build` | 改完重建 |
| 运行时观测 | `device_toolbox` + Frida 技能（`frida-scripts` / `noroot-hook`） | hook、trace、dump |
| 任意命令 / 脚本 | `execute_command` / `execute_python` | jadx、apktool、binwalk、自写解析 |

`mt_apk_*` 系列需要先 `mt_apk_session_create` 建会话，后续工具都作用在该会话上。

## 证据工作流

### 1. summary 先行

大产物不要一上来全量导出。先看清单摘要（成员数、dex 大小、so 名单），
再按线索挑少数几个深入。一个 5000 文件的归档，先读 `AndroidManifest.xml` 和一个
可疑 dex，通常就能定性；全解只会淹没重点。

### 2. 每一步留痕

- 记下**文件大小、哈希、偏移**，别只记结论。版本对比时这些是唯一能对齐的东西。
- 反编译输出存文件（`execute_command` 重定向），不要靠上下文记忆翻回去。
- 覆盖率要诚实：`mt_apk_search` 只搜了 dex 不等于"整个包里没有"。没搜的地方要说没搜。

### 3. 静态与运行时分开说

静态能看到的是**代码存在**，不是**代码被执行**。`System.loadLibrary` 出现在 smali 里，
只能说明"声明了要加载"，实际加载与否要运行时确认（`frida` 的 `dlopen` hook、`/proc/<pid>/maps`）。

同理：反射、动态代理、字符串解密、`DexClassLoader` 加载的代码，静态一律视为**未知**。

### 4. 质疑"看起来对"

最常踩的坑是把相邻的东西当因果：

- 字符串 `AES/CBC/PKCS5Padding` 出现在某个类 ≠ 那个类在做加解密（可能只是常量池共享）
- 方法名叫 `encrypt` ≠ 它加密（混淆会把名字随机化，也会故意起误导名）
- 找到了 key 字符串 ≠ 它是该算法用的 key（要跟到实际传参处）

判定标准：**能看到数据从输入流到输出流的完整路径**，才叫确认。

## 各目标的深挖路径

### Android APK

1. `apk_info` 拿包名、版本、minSdk、权限、签名
2. `mt_apk_session_create` 建会话 → `mt_apk_list` 看 dex/so/assets 规模
3. `arsc_read` 读清单与关键字符串（接口地址、加密常量常在这里）
4. `mt_apk_dex_read_class_outline` 找入口与可疑类 → `dex_smali` 看方法体
5. `dex_xref` 反查谁调用了它（确认它真的在关键路径上）
6. 有 so → `mt_apk_native_inspect` 看导出符号 → `mt_apk_native_disassemble` 看关键函数
7. 需要动态确认 → Frida hook（参考 `frida-scripts` 技能）
8. 要验证改造 → `mt_apk_build` 重建 + `apk_sign` 签名 + 装到设备验

细节见 [references/android-applications.md](references/android-applications.md)（REA 原文，
其中 JADX MCP 服务配置部分在星辰不适用，读方法论即可）。

### 原生 / .so / ELF

1. `mt_apk_native_inspect`：架构、位数、是否 stripped
2. `mt_apk_native_list_items`：导入表能直接暴露它用了什么（`SSL_write`、`AES_encrypt`、
   `pthread_create` 都是强线索）
3. 找字符串：`mt_apk_search` 搜 URL、日志、错误提示 —— 报错文案常紧邻关键函数
4. `mt_apk_native_disassemble` 反汇编；`mt_apk_native_xref` 追调用者
5. JNI 边界：Java 侧 `native` 声明的方法名与 so 里 `Java_<包>_<类>_<方法>` 对应；
   动态注册看 `JNI_OnLoad` 里的 `RegisterNatives`

详见 [references/native-and-artifacts.md](references/native-and-artifacts.md)。

### JavaScript / Electron / 前端包

1. 有 `.map` 先还原源码（最省力，别硬读压缩代码）
2. 没有则 `mt_apk_search` 搜接口域名、`fetch(`、`XMLHttpRequest`、加密函数名
3. webpack 产物看 `__webpack_require__` 的模块映射表，能定位模块边界
4. Electron 看 `main`/`preload` 的 IPC 通道名（`ipcMain.handle`），这是权限边界

详见 [references/javascript-applications.md](references/javascript-applications.md)。

### 运行时观测

静态确认"存在"，运行时确认"发生"。hook 点优先级：

1. 系统调用 / libc（`open`、`read`、`connect`）—— 看它碰了哪些文件与地址
2. 加密库（`AES_*`、`EVP_*`）—— 直接拿明文
3. 动态加载（`dlopen`、`DexClassLoader`）—— 抓运行时解密的代码
4. 业务方法 —— 需要先定位到具体类/函数

详见 [references/runtime-observation.md](references/runtime-observation.md)。

## 输出规范

结论先行，证据跟上：

1. **一句话结论**：它怎么做的（如"登录签名 = MD5(参数按 key 排序拼接 + 固定盐)"）
2. **证据链**：文件 + 偏移/类名 + 关键指令或代码片段（贴原文，不要转述）
3. **覆盖边界**：分析了什么、没分析什么、哪些结论是推断
4. **可复现步骤**：别人照着你写的步骤能重跑出同样结果

不要写"经过深入分析，发现该应用采用了较为复杂的安全机制"——这种句子没有信息量。

## 结束条件

出现以下情况就停，不要无限深挖：

- 问题已回答（用户问"怎么签名"，拿到算法就够了，不用把整个加固拆完）
- 证据链已闭合（输入→处理→输出全程可见）
- 边际收益递减（再拆三层还是同一套逻辑）
- 撞到硬边界（强混淆 + 无符号 + 反调试拉满，明确告知成本，让用户决定是否继续）

## references 说明

`references/` 下 8 篇是 REA 上游原文，讲各类型产物的分析路径与技术细节，内容是可靠的。
注意两点：

- 里面的工具名（`open_binary` / `inspect_artifact` 等）是 REA 服务的，**按上面的映射表换成星辰工具**
- 涉及 REA CLI 参数、JADX MCP JAR 配置、provider 安装的部分**不适用于星辰**，跳过

需要具体技术细节时按目标类型读对应那篇，不要一次全读。
