#!/usr/bin/env python3
"""apk_so.py — 独立 .so / APK 内 lib/<abi>/*.so 两条工作流。

用法:
  apk_so.py list  <目标>              # APK -> 列出所有 native 库（按 ABI 分组）
  apk_so.py get   <目标> <entry> [out]  # 从 APK 抽出指定 lib/<abi>/x.so
  apk_so.py info  <目标> [entry]       # standalone .so 或 APK 内条目 -> ELF 摘要
  apk_so.py all   <目标> [outdir]      # 抽出全部 .so

<目标> 可以是:
  - 一个 .so 文件（独立工作流）
  - 一个 .apk（APK 内工作流）
"""
import sys, os, zipfile, hashlib, struct

SHT = {0: "NULL", 1: "PROGBITS", 2: "SYMTAB", 3: "STRTAB", 4: "RELA",
       5: "HASH", 6: "DYNAMIC", 7: "NOTE", 8: "NOBITS", 9: "REL", 11: "DYNSYM",
       14: "INIT_ARRAY", 15: "FINI_ARRAY", 16: "PREINIT_ARRAY", 17: "GROUP"}
PT = {1: "LOAD", 2: "DYNAMIC", 3: "INTERP", 4: "NOTE", 6: "PHDR", 7: "TLS",
      0x6474e550: "GNU_EH_FRAME", 0x6474e551: "GNU_STACK", 0x6474e552: "GNU_RELRO"}
EM = {183: "AArch64", 0x03: "x86", 0x3E: "x86-64", 40: "ARM", 0x08: "MIPS"}


def elf_summary(data: bytes, label: str) -> dict:
    if data[:4] != b"\x7fELF":
        return {"label": label, "error": "not an ELF"}
    is64 = data[4] == 2
    end = "<" if data[5] == 1 else ">"
    e_type, e_machine = struct.unpack_from(end + "HH", data, 16)
    out = {
        "label": label,
        "class": "ELF64" if is64 else "ELF32",
        "machine": EM.get(e_machine, hex(e_machine)),
        "type": {1: "REL", 2: "EXEC", 3: "DYN(so)", 4: "CORE"}.get(e_type, hex(e_type)),
        "sha256": hashlib.sha256(data).hexdigest()[:16],
        "size": len(data),
    }
    if is64:
        e_shoff, = struct.unpack_from(end + "Q", data, 0x28)
        e_shentsize, e_shnum, e_shstrndx = struct.unpack_from(end + "HHH", data, 0x3A)
        e_phoff, = struct.unpack_from(end + "Q", data, 0x20)
        e_phentsize, e_phnum = struct.unpack_from(end + "HH", data, 0x36)
    else:
        e_shoff, = struct.unpack_from(end + "I", data, 0x20)
        e_shentsize, e_shnum, e_shstrndx = struct.unpack_from(end + "HHH", data, 0x2E)
        e_phoff, = struct.unpack_from(end + "I", data, 0x1C)
        e_phentsize, e_phnum = struct.unpack_from(end + "HH", data, 0x2A)

    # section headers
    secs = []
    for i in range(min(e_shnum, 200)):
        off = e_shoff + i * e_shentsize
        if is64:
            nm, ty, flags, addr, offset, size = struct.unpack_from(end + "IIQQQQ", data, off)
        else:
            nm, ty, flags, addr, offset, size = struct.unpack_from(end + "IIIIII", data, off)
        secs.append((nm, ty, flags, addr, offset, size))
    # shstrtab
    names = b""
    if e_shstrndx < len(secs):
        _, _, _, _, off, size = secs[e_shstrndx]
        names = data[off:off + size]

    def sname(n):
        if not names:
            return ""
        e = names.find(b"\0", n)
        return names[n:e].decode("utf-8", "replace")

    out["sections"] = [(sname(s[0]), SHT.get(s[1], str(s[1])), s[5]) for s in secs if s[1] != 0]
    out["needed"] = []
    for _, ty, _, _, off, size in secs:
        if ty == 6 and size:  # DYNAMIC
            for j in range(0, size, 16 if is64 else 8):
                if is64:
                    tag, val = struct.unpack_from(end + "qQ", data, off + j)
                else:
                    tag, val = struct.unpack_from(end + "iI", data, off + j)
                if tag == 0:
                    break
                if tag == 1:  # DT_NEEDED
                    strtab = next((s for s in secs if s[1] == 3), None)
                    if strtab:
                        _, _, _, _, soff, ssize = strtab
                        blob = data[soff:soff + ssize]
                        e = blob.find(b"\0", val)
                        out["needed"].append(blob[val:e].decode("utf-8", "replace"))
    # symbol counts
    for _, ty, _, _, off, size in secs:
        if ty in (2, 11):
            cnt = size // (24 if is64 else 16)
            out["symbols"] = out.get("symbols", 0) + cnt
    return out


def print_summary(s: dict, indent=""):
    if "error" in s:
        print(f"{indent}{s['label']}: {s['error']}")
        return
    print(f"{indent}{s['label']}")
    print(f"{indent}  {s['class']} / {s['machine']} / {s['type']} / {s['size']} bytes / sha256:{s['sha256']}")
    if s.get("needed"):
        print(f"{indent}  NEEDED: {', '.join(s['needed'])}")
    if s.get("symbols"):
        print(f"{indent}  symbols: {s['symbols']}")


def apk_libs(path):
    with zipfile.ZipFile(path) as z:
        return [i for i in z.infolist()
                if not i.is_dir() and i.filename.startswith("lib/")
                and i.filename.endswith(".so") and i.filename.count("/") == 2]


def cmd_list(target):
    libs = apk_libs(target)
    if not libs:
        print(f"{target}: 没有 lib/<abi>/*.so")
        return
    byabi = {}
    for i in libs:
        _, abi, name = i.filename.split("/")
        byabi.setdefault(abi, []).append((name, i.file_size))
    print(f"{target}: {len(libs)} 个 native 库, {len(byabi)} 个 ABI")
    for abi in sorted(byabi):
        print(f"  [{abi}] ({len(byabi[abi])})")
        for n, sz in sorted(byabi[abi]):
            print(f"    lib/{abi}/{n:32s} {sz:>12,} bytes")


def cmd_get(target, entry, out=None):
    with zipfile.ZipFile(target) as z:
        data = z.read(entry)
    out = out or os.path.basename(entry)
    open(out, "wb").write(data)
    print(f"extracted {entry} -> {out} ({len(data)} bytes)")
    print_summary(elf_summary(data, entry), "  ")


def cmd_info(target, entry=None):
    if target.lower().endswith(".apk"):
        if not entry:
            print("APK 需要指定 entry，例如 lib/arm64-v8a/libx.so")
            return
        with zipfile.ZipFile(target) as z:
            data = z.read(entry)
        print_summary(elf_summary(data, f"{os.path.basename(target)}!{entry}"))
    else:
        print_summary(elf_summary(open(target, "rb").read(), target))


def cmd_all(target, outdir="apk_libs"):
    libs = apk_libs(target)
    os.makedirs(outdir, exist_ok=True)
    with zipfile.ZipFile(target) as z:
        for i in libs:
            dest = os.path.join(outdir, i.filename.replace("/", "_"))
            open(dest, "wb").write(z.read(i.filename))
            print_summary(elf_summary(open(dest, "rb").read(), i.filename), "  ")
    print(f"\n{len(libs)} 个库 -> {outdir}/")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(2)
    cmd, tgt = sys.argv[1], sys.argv[2]
    try:
        if cmd == "list":
            cmd_list(tgt)
        elif cmd == "get":
            cmd_get(tgt, sys.argv[3], sys.argv[4] if len(sys.argv) > 4 else None)
        elif cmd == "info":
            cmd_info(tgt, sys.argv[3] if len(sys.argv) > 3 else None)
        elif cmd == "all":
            cmd_all(tgt, sys.argv[3] if len(sys.argv) > 3 else "apk_libs")
        else:
            print(__doc__)
    except (KeyError, zipfile.BadZipFile) as e:
        print(f"ERROR: {e}", file=sys.stderr)
        sys.exit(1)
