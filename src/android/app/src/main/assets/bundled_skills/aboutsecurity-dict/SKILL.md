---
name: aboutsecurity-dict
description: "渗透字典库（精简版）：默认口令、设备出厂密码、端口服务字典、Web 目录、中间件（含 Shiro key）、Webshell 密码、通用口令等 271 个文件 / 4.4MB。用于查表验证而非大规模爆破。"
category: 字典库
author: wgpsec
source: https://github.com/wgpsec/AboutSecurity/tree/main/Dic
version: "1.0.0"
---

# AboutSecurity 字典库

## 使用方式

字典在 `dict/<分类>/...`。手机端适合做**精确验证**（查某个 key/口令是否在表内、少量尝试），不适合跑百万级爆破——那类字典已剔除。

```
grep -c -F "<待查字符串>" /builtin_skills/aboutsecurity-dict/dict/web/middleware/shiro/key.txt
```

## 清单

### _meta.yaml（1 个 / 0.0MB）

| 文件 | 大小 | 行数 |
|---|---|---|
| `_meta.yaml` | 411B | - |

### auth（46 个 / 0.6MB）

| 文件 | 大小 | 行数 |
|---|---|---|
| `auth/_meta.yaml` | 424B | - |
| `auth/credential-pair.txt` | 10KB | 700 |
| `auth/device-default/README.md` | 1KB | - |
| `auth/device-default/_meta.yaml` | 1KB | - |
| `auth/device-default/oa-system-default.txt` | 77B | 5 |
| `auth/device-default/security-appliance-default.txt` | 564B | 34 |
| `auth/flask/_meta.yaml` | 478B | - |
| `auth/flask/flask-secret-key.txt` | 195B | 12 |
| `auth/jwt/_meta.yaml` | 412B | - |
| `auth/jwt/jwt-secret.txt` | 431B | 42 |
| `auth/password/_meta.yaml` | 2KB | - |
| `auth/password/complex/_meta.yaml` | 4KB | - |
| `auth/password/complex/complex-6char-all-four-rules.txt` | 34KB | 2972 |
| `auth/password/complex/complex-8char-all-four-rules.txt` | 31KB | 2548 |
| `auth/password/complex/complex-top100.txt` | 1010B | 103 |
| `auth/password/complex/complex-top1000.txt` | 14KB | 1298 |
| `auth/password/complex/complex-top30.txt` | 326B | 34 |
| `auth/password/complex/readme.md` | 425B | - |
| `auth/password/pass-admin.txt` | 7KB | 585 |
| `auth/password/password-top10.txt` | 78B | 9 |
| `auth/password/password-top100.txt` | 1KB | 172 |
| `auth/password/password-top1000.txt` | 16KB | 1887 |
| `auth/password/password-top20000.txt` | 161KB | 19314 |
| `auth/password/sql.txt` | 2KB | 120 |
| `auth/password/wpa/_meta.yaml` | 856B | - |
| `auth/password/wpa/top447-wpa.txt` | 4KB | 446 |
| `auth/password/wpa/top4800-wpa.txt` | 44KB | 4799 |
| `auth/password/wpa/top62-wpa.txt` | 572B | 61 |
| `auth/username/_meta.yaml` | 1KB | - |
| `auth/username/cn-email.txt` | 5KB | 308 |
| `auth/username/cn-phone.txt` | 4KB | 344 |
| `auth/username/pinyin/_meta.yaml` | 2KB | - |
| `auth/username/pinyin/cn-firstname.txt` | 685B | 127 |
| `auth/username/pinyin/cn-fullname-14k.txt` | 121KB | 14390 |
| `auth/username/pinyin/cn-fullname-1k.txt` | 9KB | 1067 |
| `auth/username/pinyin/cn-surname.txt` | 500B | 116 |
| `auth/username/pinyin/lastname.txt` | 819B | 188 |
| `auth/username/pinyin/pinyin-abbrev.txt` | 10KB | 1442 |
| `auth/username/pinyin/pinyin-dot-fullname.txt` | 10KB | 1132 |
| `auth/username/pinyin/pinyin-firstname.txt` | 10KB | 1402 |
| `auth/username/pinyin/pinyin-fullname.txt` | 9KB | 990 |
| `auth/username/pinyin/pinyin-initials.txt` | 775B | 216 |
| `auth/username/user-mail.txt` | 404B | 58 |
| `auth/username/user-us.txt` | 17KB | 2091 |
| `auth/username/user-web.txt` | 855B | 112 |
| `auth/username/username-top10000.txt` | 73KB | 10414 |

### network（6 个 / 0.0MB）

| 文件 | 大小 | 行数 |
|---|---|---|
| `network/_meta.yaml` | 770B | - |
| `network/dns-servers.txt` | 118B | 9 |
| `network/exclude.txt` | 250B | 17 |
| `network/local-ip.txt` | 824B | 54 |
| `network/subdomain/_meta.yaml` | 749B | - |
| `network/subdomain/subdomain-common.txt` | 778B | 150 |

### port（62 个 / 0.1MB）

| 文件 | 大小 | 行数 |
|---|---|---|
| `port/_meta.yaml` | 535B | - |
| `port/db2/_meta.yaml` | 437B | - |
| `port/db2/pass.txt` | 15B | 1 |
| `port/db2/user.txt` | 32B | 3 |
| `port/esxi/_meta.yaml` | 441B | - |
| `port/esxi/pass.txt` | 376B | 45 |
| `port/esxi/user.txt` | 29B | 4 |
| `port/ftp/_meta.yaml` | 425B | - |
| `port/ftp/pass.txt` | 682B | 113 |
| `port/ftp/user.txt` | 68B | 11 |
| `port/memcached/_meta.yaml` | 445B | - |
| `port/memcached/pass.txt` | 262B | 32 |
| `port/memcached/user.txt` | 19B | 3 |
| `port/mongodb/_meta.yaml` | 442B | - |
| `port/mongodb/pass.txt` | 558B | 61 |
| `port/mongodb/user.txt` | 21B | 3 |
| `port/mysql/_meta.yaml` | 430B | - |
| `port/mysql/pass.txt` | 2KB | 232 |
| `port/mysql/user.txt` | 34B | 6 |
| `port/oracle/_meta.yaml` | 829B | - |
| `port/oracle/pass.txt` | 3KB | 510 |
| `port/oracle/sid.txt` | 4KB | 574 |
| `port/oracle/user-common.txt` | 56B | 9 |
| `port/oracle/user-extended.txt` | 3KB | 478 |
| `port/pop3/_meta.yaml` | 459B | - |
| `port/pop3/pass.txt` | 203B | 26 |
| `port/pop3/user.txt` | 14KB | 1501 |
| `port/port.txt` | 10KB | 2086 |
| `port/postgresql/_meta.yaml` | 479B | - |
| `port/postgresql/pass.txt` | 307B | 37 |
| `port/postgresql/user.txt` | 19B | 2 |
| `port/rdp/_meta.yaml` | 455B | - |
| `port/rdp/pass.txt` | 2KB | 263 |
| `port/rdp/user.txt` | 206B | 26 |
| `port/redis/_meta.yaml` | 302B | - |
| `port/redis/pass.txt` | 310B | 37 |
| `port/smtp/_meta.yaml` | 843B | - |
| `port/smtp/cn-email-username.txt` | 14KB | 1959 |
| `port/smtp/pass.txt` | 203B | 26 |
| `port/smtp/us-email-username.txt` | 17KB | 2092 |
| `port/smtp/user.txt` | 14KB | 1501 |
| `port/snmp/_meta.yaml` | 368B | - |
| `port/snmp/pass.txt` | 837B | 118 |
| `port/sqlserver/_meta.yaml` | 472B | - |
| `port/sqlserver/pass.txt` | 2KB | 197 |
| `port/sqlserver/user.txt` | 28B | 5 |
| `port/ssh/_meta.yaml` | 777B | - |
| `port/ssh/pass-common.txt` | 837B | 106 |
| `port/ssh/pass-extended.txt` | 3KB | 299 |
| `port/ssh/user-common.txt` | 36B | 5 |
| `port/ssh/user-extended.txt` | 155B | 25 |
| `port/svn/_meta.yaml` | 436B | - |
| `port/svn/pass.txt` | 177B | 21 |
| `port/svn/user.txt` | 24B | 4 |
| `port/telnet/_meta.yaml` | 452B | - |
| `port/telnet/pass.txt` | 218B | 27 |
| `port/telnet/user.txt` | 24B | 4 |
| `port/vnc/_meta.yaml` | 301B | - |
| `port/vnc/pass.txt` | 259B | 30 |
| `port/weblogic/_meta.yaml` | 484B | - |
| `port/weblogic/pass.txt` | 30B | 2 |
| `port/weblogic/user.txt` | 8B | - |

### readme.md（1 个 / 0.0MB）

| 文件 | 大小 | 行数 |
|---|---|---|
| `readme.md` | 2KB | - |

### regular（29 个 / 0.4MB）

| 文件 | 大小 | 行数 |
|---|---|---|
| `regular/_meta.yaml` | 544B | - |
| `regular/address/_meta.yaml` | 884B | - |
| `regular/address/area-code.txt` | 52KB | 4071 |
| `regular/address/cn-areas.txt` | 34KB | 2995 |
| `regular/address/cn-cities.txt` | 4KB | 341 |
| `regular/eicar.txt` | 68B | - |
| `regular/keyword/_meta.yaml` | 385B | - |
| `regular/keyword/keyword-common.txt` | 145B | 21 |
| `regular/letter/_meta.yaml` | 2KB | - |
| `regular/letter/lower-1char.txt` | 51B | 25 |
| `regular/letter/lower-2char.txt` | 2KB | 675 |
| `regular/letter/lower-3char.txt` | 69KB | 17575 |
| `regular/letter/misc.txt` | 133B | 66 |
| `regular/letter/upper-1char.txt` | 51B | 25 |
| `regular/letter/upper-2char.txt` | 2KB | 675 |
| `regular/letter/upper-3char.txt` | 69KB | 17575 |
| `regular/number/_meta.yaml` | 2KB | - |
| `regular/number/num-0-10.txt` | 22B | 10 |
| `regular/number/num-0-100.txt` | 293B | 100 |
| `regular/number/num-0-1000.txt` | 4KB | 1001 |
| `regular/number/num-0-10000.txt` | 48KB | 10000 |
| `regular/number/num-0-5000.txt` | 23KB | 5000 |
| `regular/number/num-00-99.txt` | 299B | 99 |
| `regular/number/num-000-999.txt` | 4KB | 999 |
| `regular/number/num-0000-1000.txt` | 5KB | 1000 |
| `regular/number/num-000000-010000.txt` | 68KB | 10001 |
| `regular/number/num-1000-9999.txt` | 44KB | 8999 |
| `regular/readme.md` | 1KB | - |
| `regular/seo/_meta.yaml` | 357B | - |

### web（126 个 / 3.3MB）

| 文件 | 大小 | 行数 |
|---|---|---|
| `web/_meta.yaml` | 3KB | - |
| `web/api-param/_meta.yaml` | 4KB | - |
| `web/api-param/api.txt` | 141B | 27 |
| `web/api-param/param-callback.txt` | 111B | 8 |
| `web/api-param/param-captcha.txt` | 275B | 49 |
| `web/api-param/param-common.txt` | 832B | 69 |
| `web/api-param/param-findpassword.txt` | 632B | 56 |
| `web/api-param/param-imagesize.txt` | 352B | 17 |
| `web/api-param/param-json.txt` | 73B | 11 |
| `web/api-param/param-lfi.txt` | 196B | 24 |
| `web/api-param/param-login.txt` | 119B | 13 |
| `web/api-param/param-rce.txt` | 191B | 24 |
| `web/api-param/param-redirect.txt` | 2KB | 88 |
| `web/api-param/param-register.txt` | 342B | 37 |
| `web/api-param/param-sqli.txt` | 181B | 24 |
| `web/api-param/param-ssrf.txt` | 194B | 24 |
| `web/api-param/param-xss.txt` | 207B | 26 |
| `web/api-param/top100-param-cookie.txt` | 960B | 99 |
| `web/api-param/top100-param-get.txt` | 519B | 98 |
| `web/api-param/top100-param-post.txt` | 563B | 84 |
| `web/cms/_meta.yaml` | 545B | - |
| `web/cms/thinkphp-controller.txt` | 884B | - |
| `web/cms/wordpress.txt` | 5KB | 189 |
| `web/ctf/_meta.yaml` | 690B | - |
| `web/ctf/param.txt` | 2KB | 44 |
| `web/ctf/sql.txt` | 307B | 94 |
| `web/ctf/uri.txt` | 3KB | 222 |
| `web/directory/_meta.yaml` | 2KB | - |
| `web/directory/admin-dir.txt` | 28KB | 2166 |
| `web/directory/api.txt` | 7KB | 341 |
| `web/directory/asp/_meta.yaml` | 660B | - |
| `web/directory/asp/top100-asp.txt` | 1KB | 86 |
| `web/directory/aspx/_meta.yaml` | 969B | - |
| `web/directory/aspx/top100-ashx.txt` | 3KB | 161 |
| `web/directory/aspx/top100-aspx.txt` | 2KB | 95 |
| `web/directory/common.txt` | 79KB | 5378 |
| `web/directory/js/_meta.yaml` | 325B | - |
| `web/directory/js/js.txt` | 3KB | 265 |
| `web/directory/jsp/_meta.yaml` | 1KB | - |
| `web/directory/jsp/action.txt` | 6KB | 268 |
| `web/directory/jsp/do-directory.txt` | 86KB | 4004 |
| `web/directory/jsp/jsp-directory.txt` | 97KB | 4217 |
| `web/directory/jsp/top100-do.txt` | 990B | 87 |
| `web/directory/jsp/top100-jsp.txt` | 1KB | 87 |
| `web/directory/login.txt` | 18KB | 1442 |
| `web/directory/payment-callback.txt` | 1KB | 73 |
| `web/directory/php/_meta.yaml` | 655B | - |
| `web/directory/php/php-directory.txt` | 74KB | 3391 |
| `web/directory/php/top100-php.txt` | 1KB | 93 |
| `web/directory/redteam-config.txt` | 1KB | 64 |
| `web/directory/redteam-file-suffix.txt` | 203B | 37 |
| `web/directory/redteam-file.txt` | 2KB | 250 |
| `web/directory/secondary/_meta.yaml` | 497B | - |
| `web/directory/secondary/secondary-common.txt` | 7KB | 718 |
| `web/directory/secondary/secondary-extended.txt` | 166KB | 24335 |
| `web/dns/_meta.yaml` | 684B | - |
| `web/dns/oob.txt` | 266B | 23 |
| `web/dns/top-dns.txt` | 1KB | 176 |
| `web/errors/_meta.yaml` | 400B | - |
| `web/errors/top100-errors.txt` | 2KB | 97 |
| `web/file-backup/_meta.yaml` | 2KB | - |
| `web/file-backup/bak-directory.txt` | 27KB | 1575 |
| `web/file-backup/bak.txt` | 10KB | 1443 |
| `web/file-backup/cfm-directory.txt` | 15KB | 247 |
| `web/file-backup/cfm.txt` | 844B | 55 |
| `web/file-backup/cgi-directory.txt` | 3KB | 97 |
| `web/file-backup/cgi.txt` | 1001B | 79 |
| `web/file-backup/db-directory.txt` | 18KB | 943 |
| `web/file-backup/db.txt` | 10KB | 867 |
| `web/file-backup/py.txt` | 2KB | 130 |
| `web/file-backup/suffix.txt` | 2KB | 402 |
| `web/file-backup/txt.txt` | 14KB | 1320 |
| `web/http/_meta.yaml` | 2KB | - |
| `web/http/header-auth.txt` | 124B | 7 |
| `web/http/header-mimetypes.txt` | 49KB | 1864 |
| `web/http/header-redteam.txt` | 2KB | 65 |
| `web/http/header-refer-ip.txt` | 189B | 6 |
| `web/http/header-refer.txt` | 1KB | 113 |
| `web/http/header-sessionid.txt` | 113B | 10 |
| `web/http/header-spider.txt` | 2KB | 24 |
| `web/http/methods.txt` | 248B | 32 |
| `web/http/proxifier-filter.txt` | 3KB | 175 |
| `web/http/top500-user-agent.txt` | 29KB | 447 |
| `web/middleware/_meta.yaml` | 395B | - |
| `web/middleware/cas/_meta.yaml` | 443B | - |
| `web/middleware/cas/pass.txt` | 6B | - |
| `web/middleware/cas/user.txt` | 7B | - |
| `web/middleware/middleware-pass.txt` | 225B | 24 |
| `web/middleware/rabbitmq/_meta.yaml` | 478B | - |
| `web/middleware/rabbitmq/pass.txt` | 291B | 36 |
| `web/middleware/rabbitmq/user.txt` | 36B | 5 |
| `web/middleware/shiro/_meta.yaml` | 376B | - |
| `web/middleware/shiro/key.txt` | 2469KB | 101147 |
| `web/middleware/spring/_meta.yaml` | 386B | - |
| `web/middleware/spring/spring-configuration-metadata.txt` | 59KB | 1712 |
| `web/middleware/tomcat/_meta.yaml` | 845B | - |
| `web/middleware/tomcat/basic.txt` | 3KB | 228 |
| `web/middleware/tomcat/pass.txt` | 300B | 37 |
| `web/middleware/tomcat/tomcat.txt` | 2KB | 90 |
| `web/middleware/tomcat/user.txt` | 36B | 5 |
| `web/middleware/weblogic/_meta.yaml` | 693B | - |
| `web/middleware/weblogic/pass.txt` | 387B | 43 |
| `web/middleware/weblogic/user.txt` | 81B | 10 |
| `web/middleware/weblogic/weblogic.txt` | 7KB | 349 |
| `web/middleware/zabbix/_meta.yaml` | 437B | - |
| `web/middleware/zabbix/pass.txt` | 6B | - |
| `web/middleware/zabbix/user.txt` | 18B | 2 |
| `web/service/_meta.yaml` | 940B | - |
| `web/service/druid/_meta.yaml` | 469B | - |
| `web/service/druid/pass.txt` | 27B | 3 |
| `web/service/druid/user.txt` | 20B | 3 |
| `web/service/jira/_meta.yaml` | 348B | - |
| `web/service/jira/jira.txt` | 1KB | 21 |
| `web/service/phpmyadmin/_meta.yaml` | 488B | - |
| `web/service/phpmyadmin/pass.txt` | 2KB | 266 |
| `web/service/phpmyadmin/user.txt` | 10B | 1 |
| `web/service/xxljob/_meta.yaml` | 342B | - |
| `web/service/xxljob/xxl-path.txt` | 111B | 10 |
| `web/upload/_meta.yaml` | 1KB | - |
| `web/upload/asp.txt` | 276B | 29 |
| `web/upload/content-type.txt` | 5KB | 252 |
| `web/upload/htaccess.txt` | 369B | 36 |
| `web/upload/jsp.txt` | 276B | 29 |
| `web/upload/php.txt` | 276B | 29 |
| `web/upload/upload-suffix.txt` | 158B | 33 |
| `web/webshell/_meta.yaml` | 534B | - |

## 已剔除的大字典（34 个 / 29.8MB）

以下字典动辄数 MB、上百万行，手机端爆破不现实，未内置。需要时从上游仓库取：
`https://github.com/wgpsec/AboutSecurity/tree/main/Dic`

- `web/webshell/webshell-pass.txt`（3.3MB）
- `regular/letter/upper-4char.txt`（2.2MB）
- `regular/letter/lower-4char.txt`（2.2MB）
- `auth/password/wpa/top200000-wpa.txt`（2.0MB）
- `web/directory/asp/asp-directory.txt`（1.4MB）
- `web/directory/aspx/aspx-directory.txt`（1.4MB）
- `web/dns/dns.txt`（1.2MB）
- `auth/password/complex/complex-6char-three-of-four.txt`（1.0MB）
- `web/directory/asp/asp.txt`（1.0MB）
- `regular/number/birthday.txt`（0.9MB）
- `auth/password/complex/complex-8char-three-of-four.txt`（0.9MB）
- `auth/password/password-top100000.txt`（0.8MB）
- `web/webshell/dir.txt`（0.7MB）
- `auth/password/complex/alphanum-6char.txt`（0.7MB）
- `auth/password/complex/complex-6char-upper-lower-digit-no-repeat.txt`（0.7MB）
- `auth/password/complex/complex-6char-upper-lower-digit.txt`（0.7MB）
- `regular/number/num-000000-100000.txt`（0.7MB）
- `regular/address/cn-pcas.json`（0.6MB）
- `web/directory/php/php.txt`（0.6MB）
- `auth/password/complex/alphanum-8char.txt`（0.6MB）
