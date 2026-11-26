#!/usr/bin/env python3
"""apk-recompress: re-deflate selected entries of an APK with zopfli.

Why: AGP writes DEX / resources.arsc / text assets with plain zlib deflate.
zopfli yields ~3-8% smaller deflate streams for the same bytes, which shrinks
the download while costing nothing at runtime (the installer unpacks it once).
Already-compressed assets (tgz/zip/png/so/...) gain nothing and cost minutes
of CPU, so they are copied verbatim.

Usage:
    apk_recompress.py in.apk -o out.apk [--only .dex,.arsc] [--iterations 15]

After it runs the zip is NOT aligned and the signature is gone:
    zipalign -f -p 4 out.apk aligned.apk
    apksigner sign --ks ... aligned.apk
"""
import argparse
import multiprocessing as mp
import os
import struct
import sys
import time
import zlib
import zipfile

SKIP_EXT = {
    ".tgz", ".targz", ".gz", ".zip", ".jar", ".apk", ".xz", ".zst", ".7z",
    ".bz2", ".png", ".jpg", ".jpeg", ".gif", ".webp", ".mp3", ".mp4", ".m4a",
    ".ogg", ".wav", ".ttf", ".otf", ".woff", ".woff2", ".so", ".webm", ".pdf",
}

# default targets when --only is not given
DEFAULT_ONLY = {".dex", ".arsc", ".json", ".html", ".js", ".css", ".txt", ".xml"}


class _NullCompressor:
    """Stand-in so zipfile.close() keeps our precomputed sizes."""
    def compress(self, data):
        return b""

    def flush(self):
        return b""


def raw_deflate(zf, info):
    """Return the compressed bytes of an entry without re-deflating it."""
    fp = zf.fp
    fp.seek(info.header_offset)
    head = fp.read(30)
    name_len, extra_len = struct.unpack("<HH", head[26:30])
    fp.seek(info.header_offset + 30 + name_len + extra_len)
    return fp.read(info.compress_size)


def zopfli_one(args):
    path, name, iters = args
    with zipfile.ZipFile(path) as z:
        data = z.read(name)
    t0 = time.time()
    out = zopfli_deflate(data, iters)
    return name, len(data), out, time.time() - t0


def zopfli_deflate(data, iters):
    """raw deflate stream via zopfli, across API versions."""
    import zopfli
    try:
        # official pypi zopfli >= 0.2: returns a zlib container
        import zopfli.zlib as _z
        return _z.compress(data, numiterations=iters, blocksplitting=True)[2:-4]
    except Exception:
        pass
    try:
        return zopfli.compress(data, format=zopfli.ZOPFLI_FORMAT_DEFLATE,
                               numiterations=iters)
    except Exception:
        pass
    c = zopfli.ZopfliCompressor(zopfli.ZOPFLI_FORMAT_DEFLATE,
                                numiterations=iters, blocksplitting=True)
    return c.compress(data) + c.flush()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("apk")
    ap.add_argument("-o", "--output", default=None)
    ap.add_argument("--iterations", type=int, default=15)
    ap.add_argument("--jobs", type=int, default=max(1, min(4, os.cpu_count() or 1)))
    ap.add_argument("--only", default="", help="comma exts, e.g. .dex,.arsc (default: dex/arsc/text)")
    ap.add_argument("--all-deflate", action="store_true", help="also try compressible non-default entries")
    args = ap.parse_args()

    only = {e.strip().lower() for e in args.only.split(",") if e.strip()} or DEFAULT_ONLY
    out_path = args.output or (args.apk + ".recompressed")

    with zipfile.ZipFile(args.apk) as z:
        infos = z.infolist()

    targets = []
    for i in infos:
        if i.is_dir() or i.compress_type != zipfile.ZIP_DEFLATED:
            continue
        ext = os.path.splitext(i.filename)[1].lower()
        if ext in SKIP_EXT:
            continue
        if ext in only or (args.all_deflate and i.file_size >= 4096):
            targets.append(i)

    print(f"[*] {len(infos)} entries, {len(targets)} candidates", flush=True)
    results = {}
    if targets and args.jobs > 1:
        pool = mp.Pool(args.jobs)
        try:
            for name, raw_len, out, dt in pool.imap_unordered(
                    zopfli_one, [(args.apk, t.filename, args.iterations) for t in targets]):
                results[name] = (raw_len, out)
                print(f"    {name}: {raw_len/1e6:.2f} -> {len(out)/1e6:.2f} MB "
                      f"({(1-len(out)/raw_len)*100:+.1f}%) {dt:.0f}s", flush=True)
        finally:
            pool.close()
            pool.join()
    elif targets:
        for i in targets:
            name, raw_len, out, dt = zopfli_one((args.apk, i.filename, args.iterations))
            results[name] = (raw_len, out)
            print(f"    {name}: {raw_len/1e6:.2f} -> {len(out)/1e6:.2f} MB "
                  f"({(1-len(out)/raw_len)*100:+.1f}%) {dt:.0f}s", flush=True)

    t0 = time.time()
    with zipfile.ZipFile(args.apk) as zin, zipfile.ZipFile(out_path, "w") as zout:
        for i in infos:
            data = zin.read(i.filename)
            got = results.get(i.filename)
            if got and len(got[1]) < len(raw_deflate(zin, i)):
                raw_len, comp = got
                # let zipfile handle metadata, then patch the sizes
                zi = zipfile.ZipInfo(i.filename, date_time=i.date_time)
                zi.compress_type = zipfile.ZIP_DEFLATED
                zi.external_attr = i.external_attr
                zi.internal_attr = i.internal_attr
                zi.create_system = i.create_system
                zf = zout.open(zi, "w")
                zf._fileobj.write(comp)
                zf._compressor = _NullCompressor()
                zf._compress_size = len(comp)
                zf._file_size = raw_len
                zf._crc = zlib.crc32(data) & 0xFFFFFFFF
                zf.close()
            elif i.compress_type == zipfile.ZIP_DEFLATED:
                comp = raw_deflate(zin, i)
                zi = zipfile.ZipInfo(i.filename, date_time=i.date_time)
                zi.compress_type = zipfile.ZIP_DEFLATED
                zi.external_attr = i.external_attr
                zi.internal_attr = i.internal_attr
                zi.create_system = i.create_system
                zf = zout.open(zi, "w")
                zf._fileobj.write(comp)
                zf._compressor = _NullCompressor()
                zf._compress_size = len(comp)
                zf._file_size = len(data)
                zf._crc = zlib.crc32(data) & 0xFFFFFFFF
                zf.close()
            else:
                zi = zipfile.ZipInfo(i.filename, date_time=i.date_time)
                zi.compress_type = zipfile.ZIP_STORED
                zi.external_attr = i.external_attr
                zi.internal_attr = i.internal_attr
                zi.create_system = i.create_system
                zout.writestr(zi, data)

    o, n = os.path.getsize(args.apk), os.path.getsize(out_path)
    print(f"[+] total apk: {o/1e6:.2f} MB -> {n/1e6:.2f} MB "
          f"({(1-n/o)*100:.2f}% smaller), rewrite {time.time()-t0:.0f}s")
    print(f"[!] next: zipalign -f -p 4 {out_path} aligned.apk && apksigner sign ...")


if __name__ == "__main__":
    sys.exit(main())
