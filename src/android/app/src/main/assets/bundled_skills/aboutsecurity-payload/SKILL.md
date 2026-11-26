---
name: aboutsecurity-payload
description: "攻击载荷库：SQLi、XSS、SSRF、XXE、LFI、RCE、上传绕过、CORS、HPP、格式化串、SSI、邮件头注入、访问绕过、提示词注入、反弹 shell 共 14 类的实战载荷，含变形与绕过写法。"
category: 载荷库
author: wgpsec
source: https://github.com/wgpsec/AboutSecurity/tree/main/Payload
version: "1.0.0"
---

# AboutSecurity 攻击载荷库

88 个文件。

## 使用方式

载荷在 `payload/<类型>/<文件>`。先看同目录的 `.md` 说明（多数类型都有一份，写了适用场景与变形要点），再取具体载荷文件。

```
grep -ril "<关键词>" /builtin_skills/aboutsecurity-payload/payload/ | head -20
```

## 目录清单

| 类型 | 文件 |
|---|---|
| `access-bypass` | `_meta.yaml`、`xff-403.txt` |
| `cors` | `_meta.yaml`、`cors.html`、`cors.md`、`cors2.html` |
| `email` | `_meta.yaml`、`email.txt` |
| `format` | `_meta.yaml`、`hex.txt`、`int.txt`、`json.txt`、`nullbyte.txt`、`string.txt` |
| `hpp` | `_meta.yaml`、`hpp.txt` |
| `lfi` | `_meta.yaml`、`java-path.txt`、`java-properties.txt`、`linux-path.txt`、`linux.txt`、`log.txt`、`properties.txt`、`traversal.txt`、`windows.txt` |
| `prompt-injection` | `_meta.yaml`、`prompt.md` |
| `rce` | `_meta.yaml`、`java.txt`、`php.txt`、`powershell.txt`、`rce.txt`、`unix.txt`、`windows.txt` |
| `reverse-shell` | `_meta.yaml`、`bash.txt`、`others.txt`、`powershell.txt`、`python.txt`、`upgrade.txt` |
| `sqli` | `_meta.yaml`、`column.txt`、`database.txt`、`payload-blind.txt`、`payload-ldap.txt`、`payload-mssql.txt`、`payload-mysql.txt`、`payload-oracle.txt`、`payload.txt`、`sql-inj.md` 等 11 个 |
| `ssi` | `_meta.yaml`、`payload.txt` |
| `ssrf` | `_meta.yaml`、`payload.txt` |
| `upload` | `1.doc`、`1.html`、`1.pdf`、`1.png`、`1.svg`、`1.txt`、`1.xlsx`、`1.zip`、`_meta.yaml`、`upload.html` 等 11 个 |
| `xss` | `1.html`、`1.pdf`、`1.svg`、`2.svg`、`_meta.yaml`、`_meta.yaml`、`_meta.yaml`、`_meta.yaml`、`_meta.yaml`、`js-event.txt` 等 19 个 |
| `xxe` | `_meta.yaml`、`xml.txt`、`xxe.md` |
