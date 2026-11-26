---
name: aboutsecurity-index
description: "AboutSecurity 渗透知识库总导航：249 个方法论技能（AI 安全/云/代码审计/CTF/取证/规避/利用/横向/移动/后渗透/信息收集/威胁情报/工具）+ 漏洞库 + 载荷库 + 字典库。当需要判断该用哪个技能、或要在渗透测试全链条中定位下一步时先读这个。"
category: 知识库导航
author: wgpsec
source: https://github.com/wgpsec/AboutSecurity
version: "1.0.0"
---

# AboutSecurity 导航

四套内容，按需取用：

| 模块 | 技能名 | 内容 |
|---|---|---|
| 方法论 | 本表 249 个技能 | 从信息收集到后渗透的完整链路打法 |
| 漏洞数据 | `aboutsecurity-vuln` | 结构化产品/CVE 条目：影响版本、指纹、PoC |
| 攻击载荷 | `aboutsecurity-payload` | 14 类载荷与变形写法 |
| 字典 | `aboutsecurity-dict` | 默认口令、目录、中间件 key 等 |

先按目标特征选技能；确认到具体产品与版本后，转 `aboutsecurity-vuln` 查对应 CVE。

## 技能总表

### AI 安全（9）

- `agent-security` — `skills/ai-security/agent-security/`
- `ai-data-security` — `skills/ai-security/ai-data-security/`
- `ai-identity-security` — `skills/ai-security/ai-identity-security/`
- `cot-injection` — `skills/ai-security/cot-injection/`
- `cross-domain-attack-chain` — `skills/ai-security/cross-domain-attack-chain/`
- `mcp-security` — `skills/ai-security/mcp-security/`
- `prompt-injection` — `skills/ai-security/prompt-injection/`
- `prompt-jailbreak` — `skills/ai-security/prompt-jailbreak/`
- `prompt-leak` — `skills/ai-security/prompt-leak/`

### 云安全（27）

- `aliyun-pentesting` — `skills/cloud/aliyun-pentesting/`
- `aws-iam-policy-analysis` — `skills/cloud/aws-iam-policy-analysis/`
- `aws-iam-privesc` — `skills/cloud/aws-iam-privesc/`
- `aws-pentesting` — `skills/cloud/aws-pentesting/`
- `aws-post-exploit` — `skills/cloud/aws-post-exploit/`
- `azure-ad-attack` — `skills/cloud/azure-ad-attack/`
- `azure-hybrid-lateral` — `skills/cloud/azure-hybrid-lateral/`
- `azure-pentesting` — `skills/cloud/azure-pentesting/`
- `cicd-pipeline-attack` — `skills/cloud/cicd-pipeline-attack/`
- `cloud-aksk-exploit` — `skills/cloud/cloud-aksk-exploit/`
- `cloud-iam-audit` — `skills/cloud/cloud-iam-audit/`
- `cloud-metadata` — `skills/cloud/cloud-metadata/`
- `docker-pentesting` — `skills/cloud/docker-pentesting/`
- `gcp-exploit` — `skills/cloud/gcp-exploit/`
- `gcp-pentesting` — `skills/cloud/gcp-pentesting/`
- `gcp-workspace-pivot` — `skills/cloud/gcp-workspace-pivot/`
- `huawei-pentesting` — `skills/cloud/huawei-pentesting/`
- `k8s-container-escape` — `skills/cloud/k8s-container-escape/`
- `k8s-ingress-nightmare` — `skills/cloud/k8s-ingress-nightmare/`
- `k8s-istio-bypass` — `skills/cloud/k8s-istio-bypass/`
- `k8s-network-recon` — `skills/cloud/k8s-network-recon/`
- `k8s-sidecar-attack` — `skills/cloud/k8s-sidecar-attack/`
- `k8s-storage-exploit` — `skills/cloud/k8s-storage-exploit/`
- `k8s-webhook-abuse` — `skills/cloud/k8s-webhook-abuse/`
- `oss-bucket-exploit` — `skills/cloud/oss-bucket-exploit/`
- `serverless-attack` — `skills/cloud/serverless-attack/`
- `tencent-pentesting` — `skills/cloud/tencent-pentesting/`

### 代码审计（16）

- `java-audit-pipeline` — `skills/code-audit/java/java-audit-pipeline/`
- `java-auth-config-audit` — `skills/code-audit/java/java-auth-config-audit/`
- `java-exploit-chain` — `skills/code-audit/java/java-exploit-chain/`
- `java-file-audit` — `skills/code-audit/java/java-file-audit/`
- `java-framework-audit` — `skills/code-audit/java/java-framework-audit/`
- `java-frontend-audit` — `skills/code-audit/java/java-frontend-audit/`
- `java-injection-audit` — `skills/code-audit/java/java-injection-audit/`
- `java-serialization-audit` — `skills/code-audit/java/java-serialization-audit/`
- `php-audit-pipeline` — `skills/code-audit/php/php-audit-pipeline/`
- `php-auth-config-audit` — `skills/code-audit/php/php-auth-config-audit/`
- `php-exploit-chain` — `skills/code-audit/php/php-exploit-chain/`
- `php-file-audit` — `skills/code-audit/php/php-file-audit/`
- `php-framework-audit` — `skills/code-audit/php/php-framework-audit/`
- `php-frontend-audit` — `skills/code-audit/php/php-frontend-audit/`
- `php-injection-audit` — `skills/code-audit/php/php-injection-audit/`
- `php-serialization-audit` — `skills/code-audit/php/php-serialization-audit/`

### CTF（14）

- `ctf-ai-ml` — `skills/ctf/ctf-ai-ml/`
- `ctf-crypto` — `skills/ctf/ctf-crypto/`
- `ctf-flag-hunting` — `skills/ctf/ctf-flag-hunting/`
- `ctf-flag-verification` — `skills/ctf/ctf-flag-verification/`
- `ctf-forensics` — `skills/ctf/ctf-forensics/`
- `ctf-malware` — `skills/ctf/ctf-malware/`
- `ctf-misc` — `skills/ctf/ctf-misc/`
- `ctf-osint` — `skills/ctf/ctf-osint/`
- `ctf-pwn` — `skills/ctf/ctf-pwn/`
- `ctf-reverse` — `skills/ctf/ctf-reverse/`
- `ctf-solve` — `skills/ctf/ctf-solve/`
- `ctf-source-audit` — `skills/ctf/ctf-source-audit/`
- `ctf-web-methodology` — `skills/ctf/ctf-web-methodology/`
- `ctf-web-recon` — `skills/ctf/ctf-web-recon/`

### 取证响应（4）

- `disk-forensics-evasion` — `skills/dfir/disk-forensics-evasion/`
- `emergency-response` — `skills/dfir/emergency-response/`
- `log-evasion` — `skills/dfir/log-evasion/`
- `memory-forensics-evasion` — `skills/dfir/memory-forensics-evasion/`

### 规避对抗（4）

- `c2-evasion-methodology` — `skills/evasion/c2-evasion-methodology/`
- `evasion-research` — `skills/evasion/evasion-research/`
- `evasion-technique-integrate` — `skills/evasion/evasion-technique-integrate/`
- `shellcode-loader-generate` — `skills/evasion/shellcode-loader-generate/`

### 漏洞利用（76）

- `401-403-bypass` — `skills/exploit/auth/401-403-bypass/`
- `ai-infrastructure-attack` — `skills/exploit/advanced/ai-infrastructure-attack/`
- `api-fuzz` — `skills/exploit/web-method/api-fuzz/`
- `binary-exploitation-methodology` — `skills/exploit/binary/binary-exploitation-methodology/`
- `binary-exploitation-tools` — `skills/exploit/binary/binary-exploitation-tools/`
- `business-logic-attack` — `skills/exploit/web-method/business-logic-attack/`
- `cache-poisoning-smuggling` — `skills/exploit/web-method/cache-poisoning-smuggling/`
- `captcha-bypass-methodology` — `skills/exploit/auth/captcha-bypass-methodology/`
- `command-injection-methodology` — `skills/exploit/web-method/command-injection-methodology/`
- `cookie-analysis` — `skills/exploit/auth/cookie-analysis/`
- `cors-misconfiguration` — `skills/exploit/auth/cors-misconfiguration/`
- `crypto-web-attack` — `skills/exploit/advanced/crypto-web-attack/`
- `csrf-methodology` — `skills/exploit/auth/csrf-methodology/`
- `deserialization-methodology` — `skills/exploit/web-method/deserialization-methodology/`
- `dns-pentesting` — `skills/exploit/network-service/dns-pentesting/`
- `elasticsearch-attack` — `skills/exploit/network-service/elasticsearch-attack/`
- `expression-language-injection` — `skills/exploit/web-method/expression-language-injection/`
- `file-upload-methodology` — `skills/exploit/web-method/file-upload-methodology/`
- `ftp-pentesting` — `skills/exploit/network-service/ftp-pentesting/`
- `graphql-methodology` — `skills/exploit/web-method/graphql-methodology/`
- `http-host-header-attacks` — `skills/exploit/web-method/http-host-header-attacks/`
- `http-smuggling-advanced` — `skills/exploit/advanced/http-smuggling-advanced/`
- `idor-methodology` — `skills/exploit/auth/idor-methodology/`
- `imap-pentesting` — `skills/exploit/network-service/imap-pentesting/`
- `information-disclosure-methodology` — `skills/exploit/web-method/information-disclosure-methodology/`
- `java-deserialization-methodology` — `skills/exploit/web-method/java-deserialization-methodology/`
- `jwt-attack-methodology` — `skills/exploit/auth/jwt-attack-methodology/`
- `kafka-attack` — `skills/exploit/network-service/kafka-attack/`
- `kerberos-pentesting` — `skills/exploit/network-service/kerberos-pentesting/`
- `ldap-injection` — `skills/exploit/web-method/ldap-injection/`
- `ldap-pentesting` — `skills/exploit/network-service/ldap-pentesting/`
- `lfi-rfi-methodology` — `skills/exploit/web-method/lfi-rfi-methodology/`
- `memcache-pentesting` — `skills/exploit/network-service/memcache-pentesting/`
- `mobile-backend` — `skills/exploit/auth/mobile-backend/`
- `mongodb-attack` — `skills/exploit/network-service/mongodb-attack/`
- `mssql-pentesting` — `skills/exploit/network-service/mssql-pentesting/`
- `mysql-pentesting` — `skills/exploit/network-service/mysql-pentesting/`
- `nosql-injection` — `skills/exploit/web-method/nosql-injection/`
- `oauth-sso-attack` — `skills/exploit/auth/oauth-sso-attack/`
- `ot-ics-attack` — `skills/exploit/advanced/ot-ics-attack/`
- `password-reset-methodology` — `skills/exploit/auth/password-reset-methodology/`
- `php-bypass` — `skills/exploit/web-method/php-bypass/`
- `php-type-juggling` — `skills/exploit/web-method/php-type-juggling/`
- `pop-pentesting` — `skills/exploit/network-service/pop-pentesting/`
- `postgresql-attack` — `skills/exploit/network-service/postgresql-attack/`
- `postgresql-pentesting` — `skills/exploit/network-service/postgresql-pentesting/`
- `privilege-escalation-web` — `skills/exploit/web-method/privilege-escalation-web/`
- `prototype-pollution-exploit` — `skills/exploit/web-method/prototype-pollution-exploit/`
- `python-prototype-pollution` — `skills/exploit/web-method/python-prototype-pollution/`
- `python-web-debug` — `skills/exploit/web-method/python-web-debug/`
- `race-condition-exploit` — `skills/exploit/advanced/race-condition-exploit/`
- `rdp-pentesting` — `skills/exploit/network-service/rdp-pentesting/`
- `redis-attack` — `skills/exploit/network-service/redis-attack/`
- `redis-pentesting` — `skills/exploit/network-service/redis-pentesting/`
- `smb-pentesting` — `skills/exploit/network-service/smb-pentesting/`
- `smtp-pentesting` — `skills/exploit/network-service/smtp-pentesting/`
- `snmp-pentesting` — `skills/exploit/network-service/snmp-pentesting/`
- `sql-injection-methodology` — `skills/exploit/web-method/sql-injection-methodology/`
- `sqlserver-attack` — `skills/exploit/network-service/sqlserver-attack/`
- `ssh-pentesting` — `skills/exploit/network-service/ssh-pentesting/`
- `ssrf-methodology` — `skills/exploit/web-method/ssrf-methodology/`
- `ssti-methodology` — `skills/exploit/web-method/ssti-methodology/`
- `subdomain-takeover` — `skills/exploit/web-method/subdomain-takeover/`
- `supply-chain-attack` — `skills/exploit/advanced/supply-chain-attack/`
- `supply-chain-audit` — `skills/exploit/advanced/supply-chain-audit/`
- `vnc-pentesting` — `skills/exploit/network-service/vnc-pentesting/`
- `voip-pentesting` — `skills/exploit/network-service/voip-pentesting/`
- `waf-bypass-methodology` — `skills/exploit/web-method/waf-bypass-methodology/`
- `web-vuln-scan` — `skills/exploit/web-method/web-vuln-scan/`
- `webshell-deploy` — `skills/exploit/web-method/webshell-deploy/`
- `webshell-management` — `skills/exploit/web-method/webshell-management/`
- `websocket-attack` — `skills/exploit/web-method/websocket-attack/`
- `winrm-pentesting` — `skills/exploit/network-service/winrm-pentesting/`
- `xslt-injection` — `skills/exploit/web-method/xslt-injection/`
- `xss-methodology` — `skills/exploit/web-method/xss-methodology/`
- `xxe-injection-methodology` — `skills/exploit/web-method/xxe-injection-methodology/`

### 通用方法（6）

- `efficiency-rules` — `skills/general/efficiency-rules/`
- `judge-ctf` — `skills/general/judge-ctf/`
- `judge-pentest` — `skills/general/judge-pentest/`
- `local-resources` — `skills/general/local-resources/`
- `red-team-assessment` — `skills/general/red-team-assessment/`
- `report-generate` — `skills/general/report-generate/`

### 硬件渗透（1）

- `firmware-analysis` — `skills/hardware/firmware-analysis/`

### 横向移动（18）

- `ad-acl-abuse` — `skills/lateral/ad-acl-abuse/`
- `ad-delegation-attack` — `skills/lateral/ad-delegation-attack/`
- `ad-domain-attack` — `skills/lateral/ad-domain-attack/`
- `ad-persistence` — `skills/lateral/ad-persistence/`
- `ad-trust-attack` — `skills/lateral/ad-trust-attack/`
- `adcs-certipy-attack` — `skills/lateral/adcs-certipy-attack/`
- `cred-spray` — `skills/lateral/cred-spray/`
- `database-lateral-pivot` — `skills/lateral/database-lateral-pivot/`
- `exchange-attack` — `skills/lateral/exchange-attack/`
- `internal-recon` — `skills/lateral/internal-recon/`
- `kerberoast-attack` — `skills/lateral/kerberoast-attack/`
- `kerberos-advanced-attack` — `skills/lateral/kerberos-advanced-attack/`
- `lateral-movement` — `skills/lateral/lateral-movement/`
- `multi-layer-network` — `skills/lateral/multi-layer-network/`
- `network-poisoning-attack` — `skills/lateral/network-poisoning-attack/`
- `ntlm-relay-attack` — `skills/lateral/ntlm-relay-attack/`
- `oa-system-attack` — `skills/lateral/oa-system-attack/`
- `sccm-mecm-attack` — `skills/lateral/sccm-mecm-attack/`

### 恶意代码（3）

- `c2-beacon-analysis` — `skills/malware/c2-beacon-analysis/`
- `malware-analysis-methodology` — `skills/malware/malware-analysis-methodology/`
- `sandbox-evasion-implement` — `skills/malware/sandbox-evasion-implement/`

### 移动安全（4）

- `android-app-pentesting` — `skills/mobile/android-app-pentesting/`
- `ios-exploiting` — `skills/mobile/ios-exploiting/`
- `ios-pentesting` — `skills/mobile/ios-pentesting/`
- `wechat-miniprogram-pentesting` — `skills/mobile/wechat-miniprogram-pentesting/`

### 后渗透（14）

- `argocd-tactics` — `skills/postexploit/product/argocd-tactics/`
- `concourse-tactics` — `skills/postexploit/product/concourse-tactics/`
- `database-tactics` — `skills/postexploit/product/database-tactics/`
- `harbor-tactics` — `skills/postexploit/product/harbor-tactics/`
- `middleware-tactics` — `skills/postexploit/product/middleware-tactics/`
- `persist-maintain` — `skills/postexploit/persist-maintain/`
- `portainer-tactics` — `skills/postexploit/product/portainer-tactics/`
- `post-exploit-linux` — `skills/postexploit/post-exploit-linux/`
- `post-exploit-windows` — `skills/postexploit/post-exploit-windows/`
- `rabbitmq-tactics` — `skills/postexploit/product/rabbitmq-tactics/`
- `servicemesh-tactics` — `skills/postexploit/product/servicemesh-tactics/`
- `sonarqube-tactics` — `skills/postexploit/product/sonarqube-tactics/`
- `terraform-tactics` — `skills/postexploit/product/terraform-tactics/`
- `tool-delivery` — `skills/postexploit/tool-delivery/`

### 信息收集（6）

- `js-api-extract` — `skills/recon/js-api-extract/`
- `passive-recon` — `skills/recon/passive-recon/`
- `recon-full` — `skills/recon/recon-full/`
- `social-engineering` — `skills/recon/social-engineering/`
- `subdomain-deep` — `skills/recon/subdomain-deep/`
- `target-profiling` — `skills/recon/target-profiling/`

### 威胁情报（3）

- `apt-emulation` — `skills/threat-intel/apt-emulation/`
- `ioc-analysis` — `skills/threat-intel/ioc-analysis/`
- `threat-hunting-evasion` — `skills/threat-intel/threat-hunting-evasion/`

### 工具使用（44）

- `adinfo-enum` — `skills/tool/adinfo-enum/`
- `bloodhound-enum` — `skills/tool/bloodhound-enum/`
- `browser-xterm-interaction` — `skills/tool/browser-xterm-interaction/`
- `ccupp-password-profiler` — `skills/tool/ccupp-password-profiler/`
- `cdk-escape` — `skills/tool/cdk-escape/`
- `cobalt-strike` — `skills/tool/cobalt-strike/`
- `dalfox-xss` — `skills/tool/dalfox-xss/`
- `dirsearch-fuzz` — `skills/tool/dirsearch-fuzz/`
- `dnsx-resolve` — `skills/tool/dnsx-resolve/`
- `ffuf-fuzz` — `skills/tool/ffuf-fuzz/`
- `fingerprintx-probe` — `skills/tool/fingerprintx-probe/`
- `fscan-scan` — `skills/tool/fscan-scan/`
- `gau-urlfetch` — `skills/tool/gau-urlfetch/`
- `githacker-git-leak` — `skills/tool/githacker-git-leak/`
- `gogo-scan` — `skills/tool/gogo-scan/`
- `hashcat-crack` — `skills/tool/hashcat-crack/`
- `httpx-probe` — `skills/tool/httpx-probe/`
- `hydra-brute` — `skills/tool/hydra-brute/`
- `impacket-toolkit` — `skills/tool/impacket-toolkit/`
- `iox-proxy` — `skills/tool/iox-proxy/`
- `john-crack` — `skills/tool/john-crack/`
- `k8spider` — `skills/tool/k8spider/`
- `katana-crawl` — `skills/tool/katana-crawl/`
- `ksubdomain-brute` — `skills/tool/ksubdomain-brute/`
- `mapcidr-cidr` — `skills/tool/mapcidr-cidr/`
- `masscan-scan` — `skills/tool/masscan-scan/`
- `msf-oneshot` — `skills/tool/msf-oneshot/`
- `mythic-c2` — `skills/tool/mythic-c2/`
- `naabu-portscan` — `skills/tool/naabu-portscan/`
- `nerva-scan` — `skills/tool/nerva-scan/`
- `nikto-scan` — `skills/tool/nikto-scan/`
- `nmap-scan` — `skills/tool/nmap-scan/`
- `nuclei-scan` — `skills/tool/nuclei-scan/`
- `nxc-cme` — `skills/tool/nxc-cme/`
- `padbuster-padding-oracle` — `skills/tool/padbuster-padding-oracle/`
- `responder-poison` — `skills/tool/responder-poison/`
- `sliver-c2` — `skills/tool/sliver-c2/`
- `spray-dir-brute` — `skills/tool/spray-dir-brute/`
- `sqlmap-advanced` — `skills/tool/sqlmap-advanced/`
- `subfinder-recon` — `skills/tool/subfinder-recon/`
- `tlsx-probe` — `skills/tool/tlsx-probe/`
- `uncover-search` — `skills/tool/uncover-search/`
- `xray-scan` — `skills/tool/xray-scan/`
- `zombie-brute` — `skills/tool/zombie-brute/`
