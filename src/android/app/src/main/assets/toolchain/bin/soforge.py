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
"""
import sys, os, subprocess, json, struct, hashlib, shutil, time

R2 = shutil.which("r2") or "/usr/bin/r2"


def r2(so, cmds, extra_args=None):
    args = [R2, "-q", "-e", "bin.relocs.apply=true"]
    if extra_args:
        args += extra_args
    args += ["-c", cmds, so]
    p = subprocess.run(args, capture_output=True, text=True, timeout=600)
    out = p.stdout
    # r2 5.9.8 在 Alpine 上有 config 断言噪声，过滤掉
    lines = [l for l in out.splitlines()
             if not l.startswith(("WARN:", "ERROR: unable to create", "ERROR: Cannot determine entrypoint"))]
    return "\n".join(lines)


def r2j(so, cmds):
    txt = r2(so, cmds)
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
    info = r2j(so, "ij") or {}
    c = info.get("bin", {}).get("class", "?")
    arch = info.get("bin", {}).get("arch", "?")
    bits = info.get("bin", {}).get("bits", "?")
    t = info.get("bin", {}).get("type", "?")
    print(f"架构: {arch} / {bits}bit / {c} / {t}")
    print(f"sha256: {hashlib.sha256(open(so,'rb').read()).hexdigest()}")
    print(f"大小: {os.path.getsize(so):,} bytes")

    # section 概况
    sec = r2j(so, "iSj") or []
    exec_secs = [s for s in sec if "x" in s.get("perm", "")]
    print(f"section: {len(sec)} (可执行 {len(exec_secs)}: {', '.join(s['name'] for s in exec_secs)})")

    # 依赖
    libs = r2j(so, "ilj") or []
    if libs:
        names = [l if isinstance(l, str) else l.get("name", "") for l in libs]
        print(f"依赖: {', '.join(n for n in names if n)}")

    # 函数
    funcs = r2j(so, "aa; aflj") or []
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
    funcs = r2j(so, "aa; aflj") or []
    funcs = [f for f in funcs if f.get("size", 0) >= min_size]
    funcs.sort(key=lambda f: f.get("offset", 0))
    print(f"{'offset':>12} {'size':>6} {'bbs':>4}  name")
    for f in funcs:
        print(f"{f.get('offset',0):>12x} {f.get('size',0):>6} {f.get('nbbs',0):>4}  {f.get('name','')}")
    print(f"\n共 {len(funcs)} 个函数")


def cmd_cfg(so, target, dot=False):
    if dot:
        print(r2(so, f"aa; s {target}; agfd"))
    else:
        g = r2j(so, f"aa; s {target}; agfj")
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
    out = r2(so, f"aa; axt {target}")
    print(out or "无引用")


def cmd_disasm(so, target, n=40):
    print(r2(so, f"aa; s {target}; pd {n}"))


def cmd_decompile(so, target):
    print(r2(so, f"aa; s {target}; pdd"))


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
    secs = r2j(so, "iSj") or []
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


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(2)
    cmd = sys.argv[1]
    if cmd == "session":
        do_session(sys.argv[2:])
        return
    so = sys.argv[2]
    rest = sys.argv[3:]

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
