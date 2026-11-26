#!/usr/bin/env python3
"""Strip GUI-only payload from jadx-*-all.jar so the bundled toolchain stays lean.

Kept (verified necessary for jadx.cli.JadxCLI):
  - jadx core, jadx cli, plugins for deobfuscation, smali/dex readers
  - org/jetbrains/kotlin (script engine is loaded during startup; removing it
    throws ClassNotFoundException: kotlin.reflect.KProperty / ScriptJvmCompilerIsolated)
  - io/reactivex, com/google (guava), org/apache, com/android/tools

Dropped (measured: identical output on a real APK, same 17 warnings):
  - jadx/gui/**            Swing application, no display server in the sandbox
  - com/formdev/**         FlatLaf look and feel (GUI theme only)
  - org/fife/**            RSyntaxTextArea (GUI editor widget only)
  - shadow/bundletool/**   only used to open .aab input
  - resources/new_api_database.ser   API level hints only

Usage: slim_jadx.py <in.jar> <out.jar>
"""
import os
import sys
import zipfile

DROP_PREFIX = (
    "jadx/gui/",
    "com/formdev/",
    "org/fife/",
    "shadow/bundletool/",
)
DROP_EXACT = frozenset({"resources/new_api_database.ser"})


def main():
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    src, dst = sys.argv[1], sys.argv[2]
    zin = zipfile.ZipFile(src)
    dropped = kept = 0
    with zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as zout:
        for info in zin.infolist():
            name = info.filename
            if name.startswith(DROP_PREFIX) or name in DROP_EXACT:
                dropped += 1
                continue
            zout.writestr(info, zin.read(info))
            kept += 1
    a, b = os.path.getsize(src), os.path.getsize(dst)
    print(
        "slim jadx: %d entries kept, %d dropped, %.1f MB -> %.1f MB"
        % (kept, dropped, a / 1048576.0, b / 1048576.0)
    )


if __name__ == "__main__":
    main()
