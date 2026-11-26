package com.openminis.app.ui.chat

/**
 * [T-chat-preset-prompts] Built-in one-tap prompt presets for the "+" menu.
 *
 * The catalogue is seeded from the reverse-engineering / mobile-security
 * workflow the user showed in the screenshot (APK 分析、加固脱壳、Frida、
 * 抓包、去广告、SO 修改、闪退修复…) and completed with the tasks that
 * workflow normally needs next, plus a couple of general-purpose helpers.
 *
 * Content is intentionally kept here in Chinese rather than as 60+ string
 * resources: these are long, domain-specific *documents* the user may want to
 * read/edit after inserting, not short UI labels. The panel chrome (title,
 * hint, menu label) is localized through strings.xml as usual. Picking a row
 * inserts [prompt] into the composer WITHOUT sending, so it can be edited.
 */
data class PresetPrompt(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val prompt: String,
    /** Stable id. Built-ins get "builtin_<index>" from [PresetPromptStore]. */
    val id: String = "",
    val isBuiltIn: Boolean = true,
)

object ChatPresetPrompts {

    val all: List<PresetPrompt> = listOf(
        PresetPrompt(
            emoji = "🔍",
            title = "全自动分析报告",
            subtitle = "请对我提供的这个 APK 做一次完整的自动化分析，并输出结构化报告…",
            prompt = """请以资深移动安全工程师的身份，对我提供的 APK 做一次完整的自动化分析，并输出结构化报告。报告须包含：
1. 应用基本信息：包名、版本、签名摘要、minSdk/targetSdk、权限清单；
2. 加固/壳识别结论与判断依据（特征文件、lib 名称、Application 类）；
3. 组件与攻击面：导出组件、Deep Link、Content Provider、隐式 Intent；
4. 网络资产：域名、接口路径、证书校验方式、明文流量；
5. 第三方 SDK 清单与隐私合规风险；
6. 敏感行为：设备信息、短信、剪贴板、动态加载代码、反射调用；
7. 关键 so 与 Java 层逻辑的定位建议（类名/函数名/字符串线索）；
8. 风险分级（高/中/低）与后续验证步骤。
请给出可直接执行的命令（aapt / apktool / jadx / frida / objection 等）及判断依据；无法静态确认的地方明确标注「需人工/动态验证」，不要臆测。""",
        ),
        PresetPrompt(
            emoji = "🛡️",
            title = "识别加固并给脱壳方案",
            subtitle = "帮我判断这个 APK 用了什么加固（360/梆梆/腾讯乐固/爱加密…）",
            prompt = """请帮我判断这个 APK 使用了哪种加固方案（360 加固保、梆梆、腾讯乐固、爱加密、娜迦、几维、Apktool 混淆或自研壳等），并给出对应的脱壳方案。要求：
1. 列出识别依据：assets/lib 下的特征 so、classes.dex 大小与入口 Application、Manifest 中的壳类名、字符串特征；
2. 判断是「一代整体加固」「二代 dex 抽取/指令抽取」还是「VMP / java2c」，说明依据；
3. 给出针对该壳的脱壳步骤（优先给出 frida-dexdump / FART / Youpk / dumpDex 的适用版本与命令），包含运行环境要求（Android 版本、root、Frida 版本）；
4. 脱壳后如何修复 dex（补齐 method code、修复 annotation、合并多个 dex）并用 jadx 验证；
5. 常见失败原因与排查清单（反调试、ptrace 检测、Frida 检测、时机不对）。
不确定的结论请标注置信度，并给出验证方法。""",
        ),
        PresetPrompt(
            emoji = "⚡",
            title = "生成 Frida 脚本",
            subtitle = "帮我写一个 Frida 脚本。需求：绕过 SSL Pinning，并过掉常见检测…",
            prompt = """请帮我写一个可直接用 `frida -U -f <包名> -l hook.js --no-pause` 加载的 Frida JavaScript 脚本，需求：
1. 绕过 SSL Pinning：覆盖 TrustManager / OkHttp CertificatePinner / TrustKit / WebViewClient.onReceivedSslError / 自实现 checkServerTrusted，兼容 Java 层与常见混淆；
2. 过掉常见环境检测：root 检测（文件/su/which）、Frida 检测（端口、/proc/self/maps、线程名、D-Bus）、模拟器检测、调试状态检测、代理/VPN 检测；
3. 打印关键调用栈与参数便于我核对，日志带统一前缀，避免刷屏；
4. 提供 spawn 与 attach 两种用法说明，以及脚本不生效时的排查清单（混淆后类名变化、多进程、64/32 位、加固壳需先 attach 到壳进程）。
代码请带必要注释；每个 hook 点说明「为什么要 hook 这里」。若某处需要按实际类名调整，请用醒目标注写出来。""",
        ),
        PresetPrompt(
            emoji = "📡",
            title = "抓包分析接口签名",
            subtitle = "我要分析这个 App 的网络请求和接口签名。请指导我用 Proxyman/Charles/mitmproxy…",
            prompt = """我要分析这个 App 的网络请求和接口签名，请给出一套完整可执行的方案：
1. 抓包环境搭建：代理证书安装（Android 7+ 系统证书 / Magisk MoveCert）、Wi-Fi 代理或 VPN 方式、mitmproxy / Charles / Proxyman 配置要点；
2. 遇到 SSL Pinning 或证书绑定时的绕过顺序（先试系统证书，再试 Frida unpinning 脚本，再试重打包改 network_security_config）；
3. 接口清单整理：URL、Method、Header、Query、Body、响应结构，标注每个接口的用途推断；
4. 签名/鉴权参数定位：如何从请求参数（sign / signature / nonce / timestamp / token）反查到 Java 层或 so 层的生成函数，给出 jadx 搜索关键字、Frida hook 模板与自调用（主动调用签名函数）思路；
5. 算法还原：若落在 native so，给出 IDA/Ghidra 定位字符串与 JNI_OnLoad、以及用 Frida hook so 导出/偏移函数的模板；
6. 输出一份可复用的请求模板（curl 或 Python requests），并说明哪些参数每次必须重新生成。
请按「先易后难」排序，并标注每一步的验证方法。""",
        ),
        PresetPrompt(
            emoji = "🧹",
            title = "去广告 / 去弹窗",
            subtitle = "帮我分析这个 App 的广告和打扰性弹窗（开屏/插屏/Banner/信息流）…",
            prompt = """请帮我分析并去除这个 App 的广告与打扰性弹窗（开屏、插屏、Banner、信息流、激励视频、更新弹窗、评分弹窗）。要求：
1. 先定位广告 SDK（穿山甲、优量汇、广点通、AdMob、Mintegral 等）及其初始化代码与广告位 ID 常量；
2. 给出三种层级的方案并比较风险：① 修改 smali 让初始化/展示方法直接返回；② Frida/Xposed hook 拦截展示与跳过倒计时；③ 改布局/资源隐藏 Banner 容器；
3. 针对开屏广告，说明如何定位 SplashActivity 与跳过逻辑，以及如何避免「跳过后再进一次」的循环；
4. 给出具体的 smali 修改示例（方法名、返回类型、插入位置）与回编译/重签名命令（apktool b / apksigner / zipalign）；
5. 提醒常见副作用：签名校验、完整性校验、广告 SDK 初始化失败导致崩溃、违规条款风险，并给出规避思路。
请只做技术分析与本地修改建议，并说明每一步如何验证是否生效。""",
        ),
        PresetPrompt(
            emoji = "🔧",
            title = "改 SO 关键函数",
            subtitle = "我要修改一个 native so 里的关键函数（比如让某个校验函数恒返回 true）…",
            prompt = """我要修改一个 native so 里的关键函数（例如让某个校验函数恒返回 true、跳过 license 检查、改变返回值）。请给出完整流程：
1. 定位：如何从 Java 层 native 声明 / JNI_OnLoad / 字符串 / 日志 定位到 so 中的目标函数与偏移（IDA / Ghidra / radare2 步骤）；
2. 判断架构（arm64-v8a / armeabi-v7a）与是否加壳/混淆（ollvm、字符串加密、反调试）；
3. 补丁方案二选一并给出具体字节：① 直接改汇编（MOV X0, #1 ; RET 对应的机器码、或 NOP 掉条件跳转），② 用 Frida Interceptor / Stalker 运行时替换（给出脚本模板）；
4. 静态补丁后如何写回 so、修复 ELF 校验、重打包、重签名、对齐，并说明哪些情况下会触发签名/完整性校验；
5. 验证：如何用日志、Frida 或单元测试确认函数行为已改变，且不破坏其他调用点。
请给出可复制的命令与十六进制字节，并标注「需按实际指令集调整」的位置。""",
        ),
        PresetPrompt(
            emoji = "📋",
            title = "用日志修复 360 闪退",
            subtitle = "用日志修复 360 加固后重打包的闪退问题…",
            prompt = """我的 APK 在 360 加固（或脱壳后重打包）之后启动即闪退，请指导我用日志定位并修复。要求：
1. 抓取有效日志：`adb logcat` 过滤进程、crash 缓冲区、tombstone（`data/tombstones/`），以及如何在 Application 最早时机插入日志；
2. 按崩溃类型分类排查并给出对应解法：
   - `ClassNotFoundException` / `NoClassDefFoundError`：脱壳 dex 未正确回填、多 dex 未合并；
   - `UnsatisfiedLinkError` / `dlopen failed`：so 架构、签名后压缩、extractNativeLibs；
   - 签名校验 / 完整性校验失败：定位 `getPackageManager().getPackageInfo(...).signatures`、签名 MD5 比对、classes.dex CRC 校验；
   - 资源/资源 ID 错乱：apktool 回编译资源、public.xml、资源混淆；
   - 反调试/ptrace 自检触发的主动退出；
3. 给出「最小复现 → 二分回退 → 逐项验证」的排查顺序，避免盲目改；
4. 给出常用 Frida 脚本片段用于绕过签名校验与打印退出调用栈；
5. 最后给出重打包/对齐/签名的标准命令序列。
请把每条日志特征与最可能的根因对应起来。""",
        ),
        PresetPrompt(
            emoji = "💎",
            title = "vip",
            subtitle = "请以技术专家的身份，协助我进行安全研究/技术分析…",
            prompt = """请以技术专家的身份，协助我进行安全研究与本地技术分析（仅限我自己拥有合法授权的样本）。任务：分析客户端中的会员/付费状态判断逻辑。
1. 定位会员状态的存储与判断位置：SharedPreferences / 数据库 / 内存单例 / 服务端下发的字段，以及界面上判断 vip 的布尔方法；
2. 区分「本地展示判断」与「服务端校验」，明确哪些能在本地改、哪些必须走服务端，避免得出错误结论；
3. 给出定位方法（jadx 搜索关键字、资源 id 反查、Frida hook 打印返回值与调用栈）与验证步骤；
4. 说明常见防护（混淆、native 判断、签名/完整性校验、服务端二次校验）及对应的分析思路；
5. 输出一份「结论 + 证据 + 置信度 + 需人工确认项」的报告。
请始终强调：仅用于授权范围内的安全研究与防御性分析，不绕过计费、不访问他人服务、不提供侵害第三方权益的操作步骤。""",
        ),
        PresetPrompt(
            emoji = "🌐",
            title = "webide",
            subtitle = "WebIDE 集成了超多的私有接口，帮我梳理并分析…",
            prompt = """这个 App 的 WebIDE / WebView 模块集成了大量私有接口（JsBridge / addJavascriptInterface / 自定义 scheme）。请帮我系统梳理：
1. 枚举所有暴露给前端的原生接口：搜索 `addJavascriptInterface`、`@JavascriptInterface`、`shouldOverrideUrlLoading`、`onJsPrompt`、自定义 URL scheme 与 WebMessage；
2. 逐个说明接口用途、入参、返回值，以及是否存在敏感能力（读文件、发请求、拉起组件、获取设备信息、执行 shell）；
3. 评估风险：JS 任意调用、域名白名单校验是否可被绕过（`url.startsWith` 的经典坑）、`file://` 域下跨域、接口鉴权缺失；
4. 给出验证方法（构造 HTML/JS 主动调用接口、Frida hook 打印参数）与修复建议（严格 host 校验、接口最小化、鉴权 token、禁用不必要接口）；
5. 输出一张「接口 → 能力 → 风险等级 → 触发条件」的表格。
不确定的地方标注「需动态验证」。""",
        ),
        PresetPrompt(
            emoji = "✍️",
            title = "签名校验与防篡改定位",
            subtitle = "帮我找出这个 App 的签名校验与防篡改逻辑…",
            prompt = """请帮我定位并分析这个 App 的签名校验与防篡改逻辑：
1. 常见形态：Java 层 `getPackageInfo(...).signatures` / `Signature.hashCode`、Native 层读取 APK 的 META-INF、classes.dex 或 so 的 CRC/哈希自校验、服务端签名比对、Play Integrity / SafetyNet；
2. 给出定位关键词与 jadx / IDA 搜索清单，以及 Frida hook 模板（hook `getPackageInfo`、hook 字符串比对函数、打印调用栈）；
3. 给出绕过思路（仅用于自己样本的分析）：替换签名获取处的返回值、NOP 校验分支、hook native 比较函数；
4. 说明重签名后必须同步处理的事项：加固壳的签名绑定、资源/Assets 校验、so 内硬编码签名值；
5. 输出「校验点清单 + 触发时机 + 绕过后的验证方法」，并标注哪些校验可能因多线程/定时检查而反复触发。""",
        ),
        PresetPrompt(
            emoji = "🔐",
            title = "SSL Pinning 绕过",
            subtitle = "这个 App 抓不到包，帮我判断证书绑定方式并给出绕过方案…",
            prompt = """这个 App 抓不到 HTTPS 包，请帮我判断它的证书绑定（SSL Pinning）方式并给出绕过方案：
1. 判断方式：network_security_config、OkHttp CertificatePinner、TrustManager 自实现、TrustKit、WebView 层、native 层（OpenSSL/BoringSSL 自校验）、双向认证 mTLS；
2. 按「先易后难」给出方案：① 系统证书 + Magisk 模块；② 改 network_security_config 后重打包；③ Frida / objection `android sslpinning disable`；④ Xposed 模块（JustTrustMe 类）；⑤ native 层需 hook so 的情况；
3. 若遇到双向认证，说明如何提取客户端证书（p12 / bks）与密码并导入代理；
4. 给出每种方案的适用条件、失败特征与排查顺序（代理是否生效、是否走了 QUIC/HTTP3、是否用了自定义协议而非 HTTPS）；
5. 给出验证方法：能看到明文请求即成功，并提示如何确认不是被降级到 HTTP。
请给出可直接执行的命令与脚本片段。""",
        ),
        PresetPrompt(
            emoji = "🧬",
            title = "协议与加密算法还原",
            subtitle = "帮我还原这个接口的签名/加密算法…",
            prompt = """请帮我还原这个 App 接口参数（sign / signature / encrypt body / token）的生成算法：
1. 从抓包样本入手：标注哪些字段每次变化、哪些固定，推断输入与输出（长度、字符集、是否 Base64/Hex）；
2. 定位生成函数：jadx 搜索参数名、URL、算法关键字（MD5/SHA/AES/RSA/HMAC/SecretKeySpec/Cipher），以及 so 层（导出函数、JNI_OnLoad、字符串）；
3. 判断是标准算法 + 自定义拼接，还是魔改/白盒（查表、S-Box、白盒 AES）；
4. 给出 Frida 主动调用模板：hook 签名函数打印入参/返回值，或 `Java.use(...).method(...)` 自调用构造任意请求；
5. 给出 Python 复现代码骨架与「与真实请求逐字节比对」的验证方法；
6. 若算法在 native so，给出用 Frida hook 偏移函数 + unidbg / qiling 辅助调用的思路。
请把「已确认」与「推测」分开写，推测部分给出验证实验。""",
        ),
        PresetPrompt(
            emoji = "📦",
            title = "反编译与关键类定位",
            subtitle = "帮我从 APK 里快速定位某个功能对应的类/方法…",
            prompt = """请帮我在这份 APK 里快速定位指定功能对应的类与方法（例如「登录」「支付」「开关」「校验」「上报」）。要求：
1. 给出一套定位顺序：字符串/资源 id → 布局文件 → Activity/Fragment → 事件监听 → 网络请求 → native 调用；
2. 给出 jadx / apktool / grep 的具体命令（搜索中文字符串、资源 name、url 片段、权限、广播 Action）；
3. 说明混淆下的应对：按调用链、按资源 id 反查、按字符串常量、按网络端点、按 Frida 运行时打印调用栈；
4. 若功能在 Flutter / React Native / UniApp / H5 里，说明如何识别（libflutter.so、assets/flutter_assets、index.android.bundle、assets/apps）并给出对应的分析路径；
5. 输出「候选类清单 + 证据 + 置信度」，并给出用 Frida hook 验证候选的方法。""",
        ),
        PresetPrompt(
            emoji = "🕵️",
            title = "权限与隐私合规审查",
            subtitle = "按个人信息保护法/工信部要求审查这个 App 的隐私合规…",
            prompt = """请以移动应用隐私合规审查的视角，对这个 APK 做一次权限与个人信息收集审查（参照《个人信息保护法》、GB/T 35273、工信部 164 号文等常见要求）。输出：
1. 权限清单：逐项标注危险权限、申请时机（是否使用前申请）、是否与功能必要；
2. 实际收集行为：设备标识符（IMEI/OAID/Android ID/MAC）、位置、通讯录、短信、剪贴板、应用列表、传感器、麦克风/相机，列出触发代码位置与调用链；
3. 第三方 SDK 清单及其收集行为、是否有隐私政策披露与单独同意；
4. 数据传输：是否明文、是否上传境外域名、是否有加密与最小必要；
5. 风险项分级 + 整改进建议（权限收敛、延迟申请、去标识化、隐私政策补充、SDK 配置开关）；
6. 给出可复核的证据（类名/方法/字符串/抓包样本）。
请区分「静态可见」与「需动态验证」，不要下无证据的结论。""",
        ),
        PresetPrompt(
            emoji = "☣️",
            title = "恶意行为与风险研判",
            subtitle = "判断这个样本是否有恶意/风险行为，给出研判报告…",
            prompt = """请对这个 APK 做恶意行为与风险研判，输出一份可用于复核的报告：
1. 静态特征：权限组合、签名、包名仿冒、加固/混淆、可疑 so 与 assets、动态加载 dex/so、反射与加密字符串；
2. 高危行为清单：短信拦截/发送、 accessibility 无障碍滥用、通知读取、设备管理器、悬浮窗劫持、静默安装/下载、远控指令、扣费/订阅、剪贴板监听、录屏；
3. 网络侧：C2 域名/IP、硬编码 URL、DNS/回连特征、非标准端口、加密配置；
4. 持久化与对抗：自启动广播、JobScheduler、前台服务、反卸载/反调试/反模拟器、多进程守护；
5. 与常见恶意家族/灰产 SDK 的特征比对，给出「恶意 / 风险 / 可疑 / 未见明显异常」的结论与置信度；
6. 给出动态验证方案（沙箱、Frida 监控、网络流量）与处置建议。
所有结论必须附证据位置；无法确认的一律列为「需动态验证」。""",
        ),
        PresetPrompt(
            emoji = "🚪",
            title = "组件暴露与攻击面",
            subtitle = "梳理导出组件、Deep Link 与跨应用攻击面…",
            prompt = """请梳理这个 APK 的组件暴露与跨应用攻击面：
1. 从 AndroidManifest 提取所有 exported 组件（Activity / Service / Receiver / Provider），标注 intent-filter、permission 保护情况；
2. Deep Link / App Link：scheme、host、path 与参数，检查是否存在参数注入、任意页面跳转、token 泄露；
3. Content Provider：`android:exported`、`grantUriPermissions`、path 前缀，检查 SQL 注入、路径穿越、任意文件读写（`openFile`）；
4. 广播与 PendingIntent：隐式广播劫持、PendingIntent 可变性/空 Intent 问题；
5. WebView 与 JsBridge 暴露面、`file://` 跨域、Intent scheme URL 引发的任意组件启动；
6. 输出「组件 → 暴露方式 → 可达条件 → 影响 → 验证用的 adb am / content / drozer 命令」表格，并给出最小修复建议。""",
        ),
        PresetPrompt(
            emoji = "🌍",
            title = "网络资产与域名梳理",
            subtitle = "从 APK 中提取所有域名、接口与 IP，做资产梳理…",
            prompt = """请从这个 APK 中系统提取网络资产并输出清单：
1. 硬编码 URL / 域名 / IP / CDN / 云存储 bucket / WebSocket 地址，包含 Java 层、so 层字符串、assets 配置、Manifest（network_security_config、Deep Link host）；
2. 归类：API 主域、统计/埋点、广告 SDK、推送、崩溃上报、配置下发、第三方登录、备用/测试域；
3. 标注协议（HTTP 明文 / HTTPS / 自定义端口）与证书绑定配置；
4. 给出提取命令（strings / grep 正则 / jadx 全量搜索 / apktool 资源）与去重合并方法；
5. 输出「资产 → 用途推断 → 风险（明文/境外/硬编码密钥/测试域泄露）→ 验证方式」表格。
请把确认与推断分开，并提示哪些需要抓包才能确认真实调用。""",
        ),
        PresetPrompt(
            emoji = "🧩",
            title = "脱壳后 dex 修复与重打包",
            subtitle = "脱出来的 dex 打不开/回编译失败，帮我修复并重打包…",
            prompt = """我脱壳拿到了 dex（可能多个、可能带 dump 后缀），现在要修复并重新打包成可安装的 APK，请给出完整流程：
1. 判断 dump 出的 dex 类型：整体 dex / 抽取型（method code 为空）/ 多 dex / 含 OAT 优化产物，说明各自的处理差异；
2. 修复：补齐 method_code、修正 dex header 的 checksum/signature、合并多个 dex、处理 annotation 与 map_list，推荐工具与命令（dexfixer、FART 修复脚本、baksmali/smali 回编译）；
3. 回填：把修复后的 dex 放回 apktool 解包目录、移除原壳的 Application 指向、恢复真实入口类、清理壳相关 so/assets；
4. 处理常见错误：`bad dex magic`、`checksum mismatch`、`method idx overflow`、多 dex 数量与 `classesN.dex` 命名、资源与 public.xml 冲突；
5. 重打包标准命令序列（apktool b → zipalign → apksigner 签名 → adb install），以及安装后如何用日志确认走到真实 Application；
6. 给出每一步的验证点与失败回退方案。""",
        ),
        PresetPrompt(
            emoji = "🧰",
            title = "Xposed / Magisk 模块方案",
            subtitle = "把这次的 hook 做成可复用的 Xposed / Magisk 模块…",
            prompt = """请把这次分析的 hook 逻辑整理成一个可长期复用的 Xposed（LSPosed）或 Magisk / Zygisk 模块方案：
1. 比较三种载体（LSPosed 模块 / Zygisk native 模块 / Frida gadget 持久化）的适用条件、优缺点与检测风险；
2. 给出 LSPosed 模块骨架：AndroidManifest 的 `xposedmodule` meta-data、`xposed_init` 入口、`IXposedHookLoadPackage` 实现、按包名过滤、findAndHookMethod 模板与混淆类的适配（按签名/字符串查找）；
3. 给出 Zygisk 场景的 native hook 思路（PLT hook / inline hook）与 JNI 交互注意点；
4. 处理多进程、64/32 位、加固壳加载时机、ClassLoader 不匹配等常见问题；
5. 输出模块目录结构、编译/打包方式与验证方法，以及「模块不生效」的排查清单。
请标注哪些部分需要按实际目标类名/方法签名替换。""",
        ),
        PresetPrompt(
            emoji = "🐞",
            title = "动态调试与断点建议",
            subtitle = "静态看不懂，帮我规划一次动态调试…",
            prompt = """静态分析卡住了，请帮我规划一次高效的动态调试：
1. 明确要验证的假设（例如「这个分支是否走到」「签名值在哪生成」），把调试目标收敛成可观测的一个函数/一个字段；
2. 选择工具并说明理由：Frida（脚本 hook / 主动调用 / Stalker）、IDA/Ghidra 远程调试 so、smali 调试（Android Studio + smalidea）、lldb，以及各自的运行环境要求；
3. 给出断点/ hook 点建议清单：入口 Activity、网络发包处、加解密函数、JNI 边界、签名校验比较点，并说明如何打印参数、返回值与调用栈；
4. 处理反调试：ptrace 占位、/proc/status、TracerPid、Frida 检测、时间差检测、多进程守护，给出对应的绕过顺序；
5. 给出「观察 → 记录 → 结论」的模板，以及如何把动态结论回填到静态分析（重命名、注释、补证据）。
请按最小代价优先排序，避免一上来就全量 trace。""",
        ),
        PresetPrompt(
            emoji = "💡",
            title = "通用 · 解释代码 / 定位问题",
            subtitle = "帮我读懂这段代码，并指出可能的问题…",
            prompt = """请帮我读懂我接下来贴出的代码，并给出：
1. 逐段说明它在做什么、关键数据流与控制流；
2. 指出潜在问题：空指针/边界、并发与线程、资源泄漏、异常吞掉、性能热点、安全隐患；
3. 给出最小可复现的验证思路或一个更小的示例；
4. 若有改进建议，给出改前/改后对比，并说明取舍与风险。
请先给结论摘要，再展开细节；不确定的地方标注「需结合上下文确认」。""",
        ),
        PresetPrompt(
            emoji = "📝",
            title = "通用 · 总结 / 翻译 / 润色",
            subtitle = "把这段内容做总结、翻译或润色，保持原意…",
            prompt = """请处理我接下来提供的内容，按以下规则输出：
1. 先给一句话结论，再给要点列表（3–7 条），保留关键数字、专有名词与结论限定条件；
2. 需要翻译时：中英互译，技术术语保留通用译法并在首次出现时括注原文，代码/命令/路径不翻译；
3. 需要润色时：只改表达不通顺与冗余处，不改变事实、语气立场与技术含义，不擅自补充原文没有的信息；
4. 若内容里有明显矛盾或疑似错误，单独列「需核对」而不是自行改写；
5. 输出保持可直接复制使用，不要添加寒暄与自我说明。""",
        ),
    )
}
