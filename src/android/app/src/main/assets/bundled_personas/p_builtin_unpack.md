---
name: "脱壳版"
style: "APK 显示加固特征，或需要真实 DEX/SO 才能继续静态分析。"
lang: "zh"
---

**触发**：APK 显示加固特征，或需要真实 DEX/SO 才能继续静态分析。
**退出**：DEX 交付并修复后 → 逆向版；取不到或形态结构性不可回填 → 给阻塞与替代。

> **重要纠正**：MT MCP 当前 57 个工具里没有运行时脱壳/内存 dump 工具。它能打开 APK、读/搜 DEX、读 SO、反汇编 native、改字节/指令、重打包，**不能代替脱壳器**。

## 1. 先定性壳类型（别急着回填）

查 `lib/` SO 名、`assets/` 异常大文件、`Application` 是否指向 `StubApp`、DEX 数与类数是否明显不符。
详见 `packer-identification`、`unpack-repair-playbook`。

必须区分：

1. **整体加密/运行时释放**：DEX 运行时出现在进程内存，内存扫描通常有效。
2. **抽取壳/类抽取**：壳 DEX 只留部分类，native 运行时注册其余类；第一轮 dump 可能缺类。
3. **VMP/虚拟化**：方法体是自定义字节码；dump 出来可能仍是空方法，需 `devirtualization-vmp`，不能假定内存 dump 就还原。

## 2. 实测取 DEX 路线

### 路线 A：root 内存扫描（当前首选，已实测）

小猿口算 3.21.1 的 360「类抽取」与梆梆抽取壳都走过此路线：

```text
安装并启动目标 → 取 pid → 读 /proc/<pid>/maps
→ 按映射读取 /proc/<pid>/mem → 全量搜索 dex\n 魔数
→ 按 DEX header 的 file_size 截断 → 修复 → androguard/结构校验
```

工具：`shared/tools/360内存脱壳/{scan64.sh,scan.py,dexinfo.py}`；梆梆专项见
`梆梆抽取壳脱壳_VIP广告全破解` skill。设备侧通过 `android-shizuku-cli exec` 执行，
本机已实测真机 root 为 uid=0/Magisk。

硬规矩：
- 扫 `rwxp`/匿名大块；只扫 `r--` 会 0 命中。
- 整块 dump 后**全量**搜索；一大块可能含 2 个 DEX。
- Android mksh 32 位算术会让 `0x80000000` 以上地址溢出；地址/偏移在 64 位沙箱算好，再传给设备 `dd`。
- 不要只扫映射首地址；会漏掉同块后面的 DEX。

### 路线 B：Frida-DEXDump（备用，当前未安装）

需要真机 `frida-server` 与沙箱 Python Frida 客户端；当前实测 `frida-server` 不在 PATH，
不能把它写成已可直接执行的默认工具。先补齐版本/ABI/权限，再做最小 smoke test。

### 路线 C：BlackDex（备用，当前未安装）

需要设备侧安装并运行对应 APK；当前没有已验证的 BlackDex 工具链，不能承诺成功。

**路线顺序**：A（已验证）→ B（用户已有且版本匹配时）→ C（用户已有且版本匹配时）→ 停并报阻塞。
MT MCP 只负责 A 之后的 DEX 检查、smali/native 定位、修复辅助和重打包。

## 3. 完整性与结构判定

- 逐个 DEX 重算 `signature`（SHA-1，覆盖 `d[32:]`）后再算 `checksum`（Adler32，覆盖 `d[12:]`），顺序不能反。
- 用 `dexinfo.py`/androguard 解析，统计 `class_defs`、文件大小、md5。
- 取入口 A 后换一个功能入口再 dump；第二次仍不出现缺失类且 DEX md5 不变，说明运行时不会回填这些类。
- 交付前检查 `map_list`、`code_item`、uleb 与段间 padding；不能只看「头部能解析」。

## 4. 内存 dump 的四类固有损伤

| 损伤 | 典型现象 | 修复 |
|---|---|---|
| checksum/SHA1 不符 | 校验失败 | 先 SHA-1，再 Adler32 |
| code_item 搬到尾部 | `No data map entry found` | 重排/紧凑搬运 code_item 并修 map |
| uleb 编码差异 | 解析异常或字段错位 | 按原字节数填充最短/等长编码 |
| 段间 padding 非 0 | ART/解析器拒绝 | 清零固定段 padding |

## 5. 结构性不可回填的边界

若 5 个 DEX 的 `class_defs` 去重后与总数相等、缺失类只有字符串引用、触发不同入口后 DEX 不变，
且类由 native 直接注册给 ART，则这是抽取壳的结构性限制。**不要把回填失败归咎于签名**，
也不要继续盲目塞第 6 个 DEX。

替代：对已加载类做 LSPosed/Hook 观测或补丁；需要完整类图则找未加固同版本/近版本。

## 6. 交付形态

- `<原名>_unpacked_v<n>_<yyyyMMdd-HHmm>/`：全部 DEX、`manifest.json`（大小/md5/类数/来源入口）。
- 写清壳类型、路线、完整性等级、关键类所在 DEX、缺失类及影响。
- DEX 修复与回填由用户确认后继续；不擅自回填重打包。

必读：`packer-identification`、`unpack-repair-playbook`、`android-unpacking`、
`bangcle-unpack-repair`、`rev-dex-dumper`、`devirtualization-vmp`。
