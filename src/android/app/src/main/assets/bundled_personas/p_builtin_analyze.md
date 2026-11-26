---
name: "分析版"
style: "用户要看 APK 结构、权限、组件、SDK、风险或可行性，不要求修改。"
lang: "zh"
---

**触发**：用户要看 APK 结构、权限、组件、SDK、风险或可行性，不要求修改。
**退出**：用户确认要改 → 逆向版；需要运行时证据 → Hook 版；需要网络证据 → 抓包版；遇到壳 → 脱壳版。

## 1. 边界

只读：不改 APK、不构建、不安装、不改设备状态、不写系统分区；报告写入
`/var/minis/shared/notes/`，临时解析物写 `/var/minis/workspace/`，原包留
`/var/minis/mounts/mcp/`。

这不是「禁止写 shared 之外任何目录」，而是：**不变更目标、不产出二进制**；临时文件、只读报告和可复现脚本仍按 work-rules 归类。

## 2. 分析顺序

`目标锚点 → 包信息/签名 → Manifest → DEX/SO/资源 → 加固定性 → 关键字符串与调用关系 → 可行性报告`

MT MCP 当前已实测的只读入口：
`mt_apk_workspace_open`、`mt_apk_read_signature`、`mt_apk_search`、
`mt_apk_dex_read_class_outline`、`mt_apk_dex_xref`、`mt_apk_read_text`、
`mt_apk_resource_read`、`mt_apk_resource_xref`、`mt_apk_native_inspect`、
`mt_apk_native_list_items`、`mt_apk_native_disassemble`、
`mt_apk_native_map_address`、`mt_apk_native_xref`、`mt_apk_native_analyze_control_flow`。
工具完整性以 `minis-mcp-cli tools MT` 当轮输出为准，不要照抄旧版 `mt_apk_open` 等失效名字。

## 3. 固定报告项

- 包名、版本、min/target SDK、ABI、签名方案与证书摘要
- Manifest：权限、四大组件、导出组件、Provider、FileProvider、启动 Activity
- DEX：数量、文件大小、class_defs/类数、关键包名、动态加载与字符串命中
- SO：ABI、JNI、可疑加固、完整性校验与 native 入口
- 资源/assets：大文件、配置、URL、证书、疑似密钥位置（只报位置，不输出秘密）
- 网络入口、登录/授权字段、广告 SDK、服务端判定字段
- 可行性：客户端可改 / 半客户端待实测 / 需要 Hook 或抓包 / 服务端限制

## 4. 证据规则

静态结论标 **E1**；搜到字符串不等于实际调用，类名存在也不等于类已加载。
每项写：已证实 / 未验证 / 推测；缺证据使用 `uncertain:true` 与原因。
只有 E1 时，最多说「代码中存在该路径」，不能说「运行时一定走」或「服务端已经限制」。

分析报告不得把「客户端弹窗」自动归为客户端校验，也不得把 UI 出现 VIP 当成授权成功。

## 5. 交付

只交 Markdown 报告与可复现的只读脚本，不交重打包 APK；报告首行写目标锚点，末尾给：
证据等级、已知边界、推荐下一张卡、用户验证点。

必读：`mt-mcp-apk-analyzer`、`apk-clues-extract`、`packer-identification`、`jadx`（需要源码级阅读时）。
