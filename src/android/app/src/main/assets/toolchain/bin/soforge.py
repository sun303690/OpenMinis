#!/usr/bin/env python3
"""soforge — ELF/SO 逆向工作台（rizin 能力 + LIEF 重建 + 编辑会话）

子命令:
  analyze   <so>                       一次性全景（架构/段/符号/函数/可疑点）
  funcs     <so> [--min N]             函数列表
  cfg       <so> <func|addr> [--dot]   控制流图
  xref      <so> <sym|addr>            交叉引用
  disasm    <so> <func|addr> [-n N]    反汇编
  decompile <so> <func|addr>           伪代码 (r2dec)
  strings   <so> [-r regex]            字符串
  search    <so> <hex|string>          字节/字符串搜索
  crypto    <so>                       加密常量扫描(AES/MD5/SHA/CRC)
  esil      <so> <addr> [-n N]         ESIL 中间表示
  sections  <so>                       section/segment 表
  patch     <so> <addr> <hexbytes> [-o out]   地址补丁
  symrename <so> <old> <new> [-o out]  符号改名
  session   <cmd> ...                  编辑会话: init/snap/undo/redo/rollback/check/audit/persist
  cache     [--purge]                  查看/清理分析缓存

全局开关 (任意位置):
  --fast     分析级别降为 aa（快，函数会少）
  --deep     分析级别升为 aaaa（最慢，实验性）
  --no-cache 不读也不写分析缓存

分析结果按 (路径, 大小, mtime, 级别) 缓存在 ~/.soforge_projects，
同一份 so 的后续命令直接 Po 载入，不再重跑 aaa。
"""
import sys, os, subprocess, json, struct, hashlib, shutil, time

R2 = shutil.which("r2") or "/usr/bin/r2"

# 分析深度：aa（快）/ aaa（默认，函数+调用+引用）/ aaaa（最慢）
# 未显式指定时按体积自动降级 —— aaa 在 11MB 的 libjvm.so 上要 2m47s，
# 60MB 级的 so 会到十几分钟，默认值不能是它。
ANALYSIS = os.environ.get("SOFORGE_ANALYSIS") or None
AUTO_THRESHOLD = int(os.environ.get("SOFORGE_AUTO_MB", "4")) * 1024 * 1024
PROJ_ROOT = os.environ.get("SOFORGE_PROJ_ROOT") or "/var/minis/workspace/.soforge_projects"
PROJ_MAX = int(os.environ.get("SOFORGE_PROJ_MAX", str(256 * 1024 * 1024)))
USE_CACHE = True
R2_TIMEOUT = int(os.environ.get("SOFORGE_TIMEOUT", "900"))


def analysis_for(so):
    """显式设置优先；否则小文件 aaa、大文件 aa（大文件 aaa 慢到不可用）。"""
    if ANALYSIS:
        return ANALYSIS
    try:
        sz = os.path.getsize(so)
    except OSError:
        return "aa"
    if sz <= AUTO_THRESHOLD:
        return "aaa"
    print(f"[i] {sz / 1048576:.1f}MB ≥ {AUTO_THRESHOLD // 1048576}MB，分析级别降为 aa（--deep 可强制加深度）",
          file=sys.stderr)
    return "aa"


def proj_path(so):
    """分析结果缓存路径。键含路径/大小/mtime/分析级别，改过的 so 自动换键。"""
    st = os.stat(so)
    raw = f"{os.path.abspath(so)}|{st.st_size}|{int(st.st_mtime)}|{analysis_for(so)}"
    key = hashlib.sha1(raw.encode()).hexdigest()[:16]
    return os.path.join(PROJ_ROOT, key + ".r2p")


def _pre_cmds(so):
    """前置命令：命中缓存用 Po 恢复，否则跑分析并 Ps 落盘。"""
    lvl = analysis_for(so)
    if not USE_CACHE:
        return lvl + "; ", None
    try:
        path = proj_path(so)
        if os.path.exists(path) and os.path.getsize(path) > 1024:
            return f"Po {path}; ", path
        os.makedirs(PROJ_ROOT, exist_ok=True)
        return f"{lvl}; Ps {path}; ", path
    except OSError:
        return lvl + "; ", None


def r2(so, cmds, extra_args=None, no_analysis=False):
    args = [R2, "-q", "-e", "bin.relocs.apply=true"]
    if extra_args:
        args += extra_args
    pre, path = ("", None) if no_analysis else _pre_cmds(so)
    args += ["-c", pre + cmds, so]
    p = subprocess.run(args, capture_output=True, text=True, timeout=R2_TIMEOUT)
    if path and os.path.exists(path) and os.path.getsize(path) > PROJ_MAX:
        try:
            os.remove(path)
            print(f"[i] 分析缓存 > {PROJ_MAX // 1048576}MB 已丢弃", file=sys.stderr)
        except OSError:
            pass
    out = p.stdout
    # r2 5.9.8 在 Alpine 上有 config 断言噪声，过滤掉
    lines = [l for l in out.splitlines()
             if not l.startswith(("WARN:", "ERROR: unable to create", "ERROR: Cannot determine entrypoint"))]
    return "\n".join(lines)


def r2j(so, cmds, no_analysis=False):
    txt = r2(so, cmds, no_analysis=no_analysis)
    txt = txt.strip()
    if not txt:
        return None
    # 取最后一个合法 JSON
    for i in range(len(txt)):
        if txt[i] in "[{":
            try:
                return json.loads(txt[i:])
            except Exception:
                continue
    return None


def cmd_analyze(so):
    print(f"=== {so} ===")
    info = r2j(so, "ij", no_analysis=True) or {}
    c = info.get("bin", {}).get("class", "?")
    arch = info.get("bin", {}).get("arch", "?")
    bits = info.get("bin", {}).get("bits", "?")
    t = info.get("bin", {}).get("type", "?")
    print(f"架构: {arch} / {bits}bit / {c} / {t}")
    print(f"sha256: {hashlib.sha256(open(so,'rb').read()).hexdigest()}")
    print(f"大小: {os.path.getsize(so):,} bytes")

    # section 概况
    sec = r2j(so, "iSj", no_analysis=True) or []
    exec_secs = [s for s in sec if "x" in s.get("perm", "")]
    print(f"section: {len(sec)} (可执行 {len(exec_secs)}: {', '.join(s['name'] for s in exec_secs)})")

    # 依赖
    libs = r2j(so, "ilj", no_analysis=True) or []
    if libs:
        names = [l if isinstance(l, str) else l.get("name", "") for l in libs]
        print(f"依赖: {', '.join(n for n in names if n)}")

    # 函数
    funcs = r2j(so, "aflj") or []
    print(f"函数: {len(funcs)}")

    # 加密常量
    hits = crypto_scan(so)
    if hits:
        print(f"可疑加密常量: {len(hits)} 处")
        for h in hits[:6]:
            print(f"  {h['offset']} {h['algo']}")


CRYPTO_SIGS = [
    ("AES S-box", "637c777bf26b6fc53001672bfed7ab76"),
    ("AES inv S-box", "52096ad53036a538bf40a39e"),
    ("MD5 IV", "0123456789abcdeffedcba9876543210"),
    ("SHA1 IV", "67452301efcdab8998badcfe10325476"),
    ("SHA256 IV", "6a09e667bb67ae853c6ef372a54ff53a"),
    ("CRC32 poly", "2083b8ed2083b8ed"),
    ("Tea delta", "b979379e"),
]


def crypto_scan(so):
    data = open(so, "rb").read()
    out = []
    for name, hexsig in CRYPTO_SIGS:
        sig = bytes.fromhex(hexsig)
        pos = 0
        while True:
            i = data.find(sig, pos)
            if i < 0:
                break
            out.append({"offset": f"0x{i:x}", "algo": name})
            pos = i + 1
    return out


def cmd_funcs(so, min_size=0):
    funcs = r2j(so, "aflj") or []
    funcs = [f for f in funcs if f.get("size", 0) >= min_size]
    funcs.sort(key=lambda f: f.get("offset", 0))
    print(f"{'offset':>12} {'size':>6} {'bbs':>4}  name")
    for f in funcs:
        print(f"{f.get('offset',0):>12x} {f.get('size',0):>6} {f.get('nbbs',0):>4}  {f.get('name','')}")
    print(f"\n共 {len(funcs)} 个函数")


def cmd_cfg(so, target, dot=False):
    if dot:
        print(r2(so, f"s {target}; agfd"))
    else:
        g = r2j(so, f"s {target}; agfj")
        if not g:
            print("未找到函数", target)
            return
        fn = g[0]
        print(f"函数 {fn.get('name')} @0x{fn.get('offset',0):x}  基本块 {len(fn.get('blocks',[]))}")
        for i, b in enumerate(fn.get("blocks", [])):
            print(f"  [块{i}] @0x{b['offset']:x} size={b['size']}")
            for op in b.get("ops", [])[:6]:
                print(f"      0x{op['offset']:x}  {op.get('disasm','')}")


def cmd_xref(so, target):
    out = r2(so, f"axt {target}")
    print(out or "无引用")


def cmd_disasm(so, target, n=40):
    print(r2(so, f"s {target}; pd {n}"))


def cmd_decompile(so, target):
    print(r2(so, f"s {target}; pdd"))


def cmd_strings(so, regex=None):
    if regex:
        print(r2(so, f"izz~/{regex}"))
    else:
        print(r2(so, "izz"))


def cmd_search(so, q):
    try:
        bytes.fromhex(q)
        print(r2(so, f"/x {q}"))
    except ValueError:
        print(r2(so, f"/ {q}"))


def cmd_esil(so, addr, n=10):
    print(r2(so, f"e asm.esil=true; s {addr}; pi {n}"))


def cmd_sections(so):
    secs = r2j(so, "iSj", no_analysis=True) or []
    print(f"{'name':24} {'addr':>12} {'vsize':>10} {'perm':>5}")
    for s in secs:
        print(f"{s.get('name',''):24} {s.get('vaddr',0):>12x} {s.get('vsize',0):>10} {s.get('perm','')}")
    segs = r2j(so, "iPj") or []
    if segs:
        print(f"\nsegment: {len(segs)}")


def cmd_patch(so, addr, hexbytes, out=None):
    import lief
    b = lief.parse(so)
    a = int(addr, 0)
    raw = bytes.fromhex(hexbytes)
    b.patch_address(a, list(raw))
    dest = out or (so.rsplit(".", 1)[0] + ".patched.so")
    b.write(dest)
    print(f"patched {len(raw)} bytes @0x{a:x} -> {dest}")
    print(f"  原始: {so} ({os.path.getsize(so):,})")
    print(f"  输出: {dest} ({os.path.getsize(dest):,})")
    # 回读验证
    v = lief.parse(dest)
    got = bytes(v.get_content_from_virtual_address(a, len(raw)))
    ok = got == raw
    print(f"  校验: {'OK' if ok else 'FAIL'} (实际 {got.hex()})")
    sess_adopt(dest, "patch", f"0x{a:x} {hexbytes}")
    return dest


def cmd_symrename(so, old, new, out=None):
    import lief
    b = lief.parse(so)
    n = 0
    for s in b.symbols:
        if s.name == old:
            s.name = new
            n += 1
    if n == 0:
        print(f"未找到符号 {old}")
        return
    dest = out or (so.rsplit(".", 1)[0] + ".renamed.so")
    b.write(dest)
    print(f"重命名 {n} 处 {old} -> {new} -> {dest}")
    sess_adopt(dest, "symrename", f"{old} -> {new}")


# ---------------- 编辑会话 ----------------

SESSION_ROOT = "/var/minis/workspace/.soforge_sessions"


def sess_dir(so):
    key = hashlib.sha256(os.path.abspath(so).encode()).hexdigest()[:12]
    d = os.path.join(SESSION_ROOT, key)
    os.makedirs(os.path.join(d, "snaps"), exist_ok=True)
    return d


def sess_state(d):
    f = os.path.join(d, "state.json")
    if os.path.exists(f):
        return json.load(open(f))
    return {"target": None, "cursor": -1, "snaps": [], "audit": []}


def sess_save(d, st):
    json.dump(st, open(os.path.join(d, "state.json"), "w"), ensure_ascii=False, indent=1)


def sess_log(d, st, action, detail=""):
    st["audit"].append({"t": time.strftime("%H:%M:%S"), "action": action, "detail": detail})
    sess_save(d, st)


def sess_adopt(path, action, detail):
    """把一次编辑的产物并入已有会话：追加快照并记 audit。

    之前 patch/symrename 只写 xxx.patched.so，会话里 anchor 永远停在 base，
    undo/rollback 实际回不到任何一次编辑。这里让编辑变成会话历史的一步。
    """
    if not os.path.isdir(SESSION_ROOT):
        return None
    tgt = os.path.abspath(path)
    for cand in sorted(os.listdir(SESSION_ROOT)):
        d = os.path.join(SESSION_ROOT, cand)
        if not os.path.isdir(d) or not os.path.exists(os.path.join(d, "state.json")):
            continue
        st = sess_state(d)
        base = st.get("target")
        if not base:
            continue
        stem = os.path.splitext(base)[0]
        if base != tgt and not tgt.startswith(stem):
            continue
        name = f"snap{len(st['snaps'])}.bin"
        st["snaps"] = st["snaps"][: st["cursor"] + 1]
        shutil.copy2(path, os.path.join(d, "snaps", name))
        st["snaps"].append({"label": f"{action} {detail}", "file": f"snaps/{name}"})
        st["cursor"] = len(st["snaps"]) - 1
        sess_log(d, st, action, detail)
        print(f"  已并入会话 #{st['cursor']} ({action})")
        return d
    return None


def do_session(argv):
    if not argv:
        print(__doc__)
        return
    sub = argv[0]

    if sub == "init":
        so = argv[1]
        d = sess_dir(so)
        st = sess_state(d)
        base = os.path.join(d, "base.bin")
        shutil.copy2(so, base)
        st.update({"target": os.path.abspath(so), "cursor": 0, "snaps": [{"label": "init", "file": "base.bin"}]})
        st["audit"] = []
        sess_save(d, st)
        sess_log(d, st, "init", so)
        print(f"会话已建立: {d}")
        print(f"  基线: {so} (sha256 {hashlib.sha256(open(so,'rb').read()).hexdigest()[:16]})")
        return

    d = None
    for cand in os.listdir(SESSION_ROOT) if os.path.isdir(SESSION_ROOT) else []:
        p = os.path.join(SESSION_ROOT, cand)
        st = sess_state(p)
        if st.get("target") and (argv[1] == st["target"] if len(argv) > 1 else False):
            d = p
            break
    if d is None:
        # 按路径推断
        if len(argv) > 1:
            d = sess_dir(argv[1])
            if not os.path.exists(os.path.join(d, "state.json")):
                print("会话不存在，先 init")
                return

    st = sess_state(d)
    cur = os.path.join(d, st["snaps"][st["cursor"]]["file"])

    if sub == "snap":
        label = argv[2] if len(argv) > 2 else ""
        name = f"snap{len(st['snaps'])}.bin"
        shutil.copy2(cur, os.path.join(d, "snaps", name))
        # 截断 redo 分支
        st["snaps"] = st["snaps"][: st["cursor"] + 1]
        st["snaps"].append({"label": label or f"snap{len(st['snaps'])}", "file": f"snaps/{name}"})
        st["cursor"] = len(st["snaps"]) - 1
        sess_log(d, st, "snapshot", label)
        print(f"快照 #{st['cursor']} {label}")
    elif sub in ("undo", "redo"):
        delta = -1 if sub == "undo" else 1
        n = int(argv[2]) if len(argv) > 2 else 1
        st["cursor"] = max(0, min(len(st["snaps"]) - 1, st["cursor"] + delta * n))
        sess_log(d, st, sub, f"-> #{st['cursor']}")
        print(f"{sub}: 现在 #{st['cursor']} {st['snaps'][st['cursor']]['label']}")
    elif sub == "rollback":
        idx = int(argv[2]) if len(argv) > 2 else -1
        idx = len(st["snaps"]) - 1 if idx < 0 else idx
        if not (0 <= idx < len(st["snaps"])):
            print("快照索引越界")
            return
        st["cursor"] = idx
        sess_log(d, st, "rollback", f"-> #{idx}")
        print(f"已回滚到 #{idx} {st['snaps'][idx]['label']}")
    elif sub == "check":
        data = open(cur, "rb").read()
        ok = data[:4] == b"\x7fELF"
        print(f"目标: {st['target']}")
        print(f"当前快照: #{st['cursor']} {st['snaps'][st['cursor']]['label']}")
        print(f"ELF 魔数: {'OK' if ok else 'BROKEN'}")
        print(f"大小: {len(data):,}  sha256: {hashlib.sha256(data).hexdigest()[:16]}")
        import lief
        try:
            b = lief.parse(cur)
            print(f"LIEF 解析: OK ({len(b.sections)} sections, {len(b.symbols)} symbols)")
        except Exception as e:
            print(f"LIEF 解析: FAIL {e}")
    elif sub == "audit":
        print(f"会话 {st['target']}")
        for a in st["audit"]:
            print(f"  {a['t']}  {a['action']:10} {a['detail']}")
    elif sub == "persist":
        dest = argv[2] if len(argv) > 2 else st["target"]
        shutil.copy2(cur, dest)
        sess_log(d, st, "persist", dest)
        print(f"已写出 -> {dest} ({os.path.getsize(dest):,} bytes)")
    else:
        print(__doc__)


def cmd_cache(argv):
    if not os.path.isdir(PROJ_ROOT):
        print("缓存目录不存在（还没有跑过分析）")
        return
    files = [os.path.join(PROJ_ROOT, f) for f in os.listdir(PROJ_ROOT) if f.endswith(".r2p")]
    if "--purge" in argv:
        n = 0
        for f in files:
            try:
                os.remove(f)
                n += 1
            except OSError:
                pass
        print(f"已清理 {n} 个分析缓存")
        return
    total = sum(os.path.getsize(f) for f in files)
    print(f"分析缓存: {len(files)} 项 / {total / 1048576:.1f} MB @ {PROJ_ROOT}")
    for f in sorted(files, key=os.path.getsize, reverse=True)[:10]:
        print(f"  {os.path.getsize(f) / 1048576:>7.1f} MB  {os.path.basename(f)}")
    print(f"当前分析级别: {ANALYSIS or f'auto (≤{AUTO_THRESHOLD // 1048576}MB: aaa, 之上: aa)'}")


def main():
    global ANALYSIS, USE_CACHE
    argv = [a for a in sys.argv[1:] if a not in ("--fast", "--deep", "--no-cache")]
    if "--fast" in sys.argv[1:]:
        ANALYSIS = "aa"
    if "--deep" in sys.argv[1:]:
        ANALYSIS = "aaaa"
    if "--no-cache" in sys.argv[1:]:
        USE_CACHE = False
    if argv and argv[0] == "cache":
        cmd_cache(argv[1:])
        return
    if len(argv) < 2:
        print(__doc__)
        sys.exit(2)
    cmd = argv[0]
    if cmd == "session":
        do_session(argv[1:])
        return
    so = argv[1]
    rest = argv[2:]

    def opt(flag, default=None):
        if flag in rest:
            i = rest.index(flag)
            if i + 1 < len(rest):
                return rest[i + 1]
        return default

    if cmd == "analyze":
        cmd_analyze(so)
    elif cmd == "funcs":
        cmd_funcs(so, int(opt("--min", "0")))
    elif cmd == "cfg":
        cmd_cfg(so, rest[0] if rest else "entry0", "--dot" in rest)
    elif cmd == "xref":
        cmd_xref(so, rest[0])
    elif cmd == "disasm":
        cmd_disasm(so, rest[0], int(opt("-n", "40")))
    elif cmd == "decompile":
        cmd_decompile(so, rest[0] if rest else "entry0")
    elif cmd == "strings":
        cmd_strings(so, opt("-r"))
    elif cmd == "search":
        cmd_search(so, rest[0])
    elif cmd == "crypto":
        hits = crypto_scan(so)
        print(f"扫描 {so}")
        for h in hits:
            print(f"  {h['offset']:>10}  {h['algo']}")
        print(f"共 {len(hits)} 处")
    elif cmd == "esil":
        cmd_esil(so, rest[0], int(opt("-n", "10")))
    elif cmd == "sections":
        cmd_sections(so)
    elif cmd == "patch":
        if len(rest) < 2:
            print("用法: patch <so> <addr> <hexbytes> [-o out]")
            sys.exit(2)
        cmd_patch(so, rest[0], rest[1], opt("-o"))
    elif cmd == "symrename":
        cmd_symrename(so, rest[0], rest[1], opt("-o"))
    else:
        print(__doc__)


if __name__ == "__main__":
    main()
