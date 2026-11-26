---
name: pojia-kb
description: 本地攻防知识库（108 条 CVE / 未授权 / 提权 / Java 反序列化 / 边界设备 / Exchange-AD / 云容器 / Web-CMS）。用户问某个 CVE、某组件的漏洞打法、指纹→漏洞映射、PoC 安全验证规范、漏洞报告模板时使用。纯本地检索，无外网。
version: 1.0.0
---

# pojia-kb — 本地攻防知识库

108 条条目，覆盖 8 个分类。每条含：指纹、前置条件、PoC（请求原文）、利用步骤、修复建议、参考。

## 调用方式

技能目录下的 `scripts/kb.py` 是唯一入口，用 shell 跑（沙箱里 python3 可用；若未装，`apk add python3`）。

```sh
KB=/var/minis/skills/pojia-kb/scripts/kb.py
```

### 1. 关键词检索

```sh
python3 $KB search nacos
python3 $KB search 提权
python3 $KB search log4j
python3 $KB search "exchange rce"
```

输出按相关度排序，最多 12 条：`score  分类/条目ID  [严重级]  标题  (CVE)`。

### 2. 读条目全文

```sh
python3 $KB read cve-2021-29441-nacos
python3 $KB read cloud-container/cve-2021-29441-nacos
python3 $KB read nacos        # 模糊匹配，取最高分那条
```

返回该条完整正文（Markdown），含可直接复现的 curl / psql / sqlmap 命令。

### 3. 列分类与全量清单

```sh
python3 $KB cats                 # 8 个分类及条目数
python3 $KB list                 # 全 108 条
python3 $KB list linux-priv      # 只看某分类
```

## 分类

| id | 中文 | 条数 |
|---|---|---|
| cloud-container | 云与容器（Nacos/K8s/Redis/Docker/MongoDB…） | 14 |
| edge-device | 边界设备（FortiOS/Pulse/Citrix/F5/Ivanti…） | 16 |
| exchange-ad | Exchange / AD（ProxyLogon/ProxyShell/Zerologon/noPac…） | 14 |
| java-deser | Java 反序列化（Shiro/Fastjson/WebLogic/JBoss…） | 9 |
| java-web | Java Web（Confluence/Jenkins/GitLab/Solr/Log4j…） | 22 |
| linux-priv | Linux 提权（DirtyPipe/DirtyCow/sudo/nf_tables…） | 11 |
| triage | 信息收集与研判（指纹映射/报告模板/验证规范） | 3 |
| web-cms | Web / CMS（Drupal/ThinkPHP/WordPress/Laravel/PHP…） | 19 |

## 使用姿势

- **先搜再读**：`search` 拿到 id → `read <id>` 拿全文。不要直接猜 id。
- **指纹先行**：不确定目标有什么洞时，先 `read triage/fingerprint-workflow`，按资产类型反查分类。
- **写报告**：`read triage/report-template` 有现成模板；`read triage/safe-verification` 是 PoC 验证分级与证据清单，涉及真实目标时先读这条。
- 条目里的 `target` / `attacker` 是占位符，替换成实际值。
- 知识库不联网，CVE 细节以条目内置文本为准；需要最新情报时另外上网查。

## 边界

这是知识库不是扫描器，不会自己打目标。执行条目里的命令前确认授权范围，默认停在 L2（只读探测），L3（写文件/加账号/反连）需要明确授权。
